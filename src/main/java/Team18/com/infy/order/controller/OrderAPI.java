package Team18.com.infy.order.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import Team18.com.infy.order.dto.CartDTO;
import Team18.com.infy.order.dto.OrderDTO;
import Team18.com.infy.order.dto.OrderPlacedDTO;
import Team18.com.infy.order.dto.ProductDTO;
import Team18.com.infy.order.exception.OrderMsException;
import Team18.com.infy.order.service.OrderService;

@RestController
public class OrderAPI {
	
	@Autowired
	private OrderService orderService;
	
//	@Autowired
//	DiscoveryClient client;
	
	@Value("${user.uri}")
	String userUri;
	
	@Value("${product.uri}")
	String productUri;
	
	@PostMapping(value = "/placeOrder/{buyerId}")
	public ResponseEntity<String> placeOrder(@PathVariable String buyerId, @RequestBody OrderDTO order){
		
		try {

			ObjectMapper mapper = new ObjectMapper();
			List<ProductDTO> productList = new ArrayList<>();
			List<CartDTO> cartList = mapper.convertValue(
					new RestTemplate().getForObject("http://localhost:9300/"+"/buyer/cart/get/" + buyerId, List.class), 
				    new TypeReference<List<CartDTO>>(){}
				);
			
			cartList.forEach(item ->{
				ProductDTO prod = new RestTemplate().getForObject("http://localhost:9200/"+"/getById/" +item.getProdId(),ProductDTO.class) ; //getByProdId/{productId}
				System.out.println(prod.getDescription());
				productList.add(prod);
			});
			
			OrderPlacedDTO orderPlaced = orderService.placeOrder(productList,cartList,order);
			cartList.forEach(item->{
				new RestTemplate().getForObject("http://localhost:9200/"+"/updateStock/" +item.getProdId()+"/"+item.getQuantity(), boolean.class) ;
				new RestTemplate().postForObject(userUri+"/buyer/cart/remove/"+buyerId+"/"+item.getProdId(),null, String.class);
			});			
			
			new RestTemplate().getForObject("http://localhost:9300/"+"/updateRewardPoints/"+buyerId+"/"+orderPlaced.getRewardPoints() , String.class);
			
			return new ResponseEntity<>(orderPlaced.getOrderId(),HttpStatus.ACCEPTED);
		}
		catch(Exception e)
		{
			String newMsg = "There was some error";
			if(e.getMessage().equals("404 null"))
			{
				newMsg = "Error while placing the order";
			}
			return new ResponseEntity<>(newMsg,HttpStatus.UNAUTHORIZED);
		}		
		
	}
	
	@GetMapping(value = "/viewAll")
	public ResponseEntity<List<OrderDTO>> viewAllOrder(){		
		try {
			List<OrderDTO> allOrders = orderService.viewAllOrders();
			return new ResponseEntity<>(allOrders,HttpStatus.OK);
		}
		catch(Exception e)
		{
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}		
	}
	
	@GetMapping(value = "/viewOrders/{buyerId}")
	public ResponseEntity<List<OrderDTO>> viewsOrdersByBuyerId(@PathVariable String buyerId){		
		try {
			List<OrderDTO> allOrders = orderService.viewOrdersByBuyer(buyerId);
			return new ResponseEntity<>(allOrders,HttpStatus.OK);
		}
		catch(Exception e)
		{
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}		
	}
	
	@GetMapping(value = "/viewOrder/{orderId}")
	public ResponseEntity<OrderDTO> viewsOrderByOrderId(@PathVariable String orderId){		
		try {
			OrderDTO allOrders = orderService.viewOrder(orderId);
			return new ResponseEntity<>(allOrders,HttpStatus.OK);
		}
		catch(Exception e)
		{
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}		
	}
	
	
	@PostMapping(value = "/reOrder/{buyerId}/{orderId}")
	public ResponseEntity<String> reOrder(@PathVariable String buyerId, @PathVariable String orderId){
		
		try {
			
			String id = orderService.reOrder(buyerId,orderId);
			return new ResponseEntity<>("Order ID: "+id,HttpStatus.ACCEPTED);
		}
		catch(Exception e)
		{
			return new ResponseEntity<>(e.getMessage(),HttpStatus.UNAUTHORIZED);
		}		
	}
	
	@PostMapping(value = "/addToCart/{buyerId}/{prodId}/{quantity}")
	public ResponseEntity<String> addToCart(@PathVariable String buyerId, @PathVariable String prodId,@PathVariable Integer quantity){
		
		try {
			
//			List<ServiceInstance> userInstances=client.getInstances("USERMS");
//			ServiceInstance userInstance=userInstances.get(0);
//			URI userUri = userInstance.getUri();
			
			String successMsg = new RestTemplate().postForObject(userUri+"/buyer/cart/add/"+buyerId+"/"+prodId+"/"+quantity, null, String.class);

			return new ResponseEntity<>(successMsg,HttpStatus.ACCEPTED);
		}
		catch(Exception e)
		{
			String newMsg = "There was some error";
			if(e.getMessage().equals("404 null"))
			{
				newMsg = "There are no PRODUCTS for the given product ID";
			}
			return new ResponseEntity<>(newMsg,HttpStatus.UNAUTHORIZED);
		}		
	}
	@GetMapping(value = "/order/status/{orderId}/{status}")
	public ResponseEntity<String> updateStatus(@PathVariable String orderId, @PathVariable String status) {
		try {

			orderService.updateOrderStatus(orderId, status);
			return new ResponseEntity<String>("Status updated successfully!", HttpStatus.OK);
		} catch (OrderMsException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}
	@PostMapping(value = "/removeFromCart/{buyerId}/{prodId}")
	public ResponseEntity<String> removeFromCart(@PathVariable String buyerId, @PathVariable String prodId){
		
		try {
			
		
			String successMsg = new RestTemplate().postForObject(userUri+"/buyer/cart/remove/"+buyerId+"/"+prodId, null, String.class);
			
			return new ResponseEntity<>(successMsg,HttpStatus.ACCEPTED);
		}
		catch(Exception e)
		{
			String newMsg = "There was some error";
			if(e.getMessage().equals("404 null"))
			{
				newMsg = "There are no PRODUCTS for the given product ID";
			}
			return new ResponseEntity<>(newMsg,HttpStatus.UNAUTHORIZED);
		}		
	}
	
	

}
