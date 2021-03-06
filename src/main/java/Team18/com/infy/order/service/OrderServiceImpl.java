package Team18.com.infy.order.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Team18.com.infy.order.dto.CartDTO;
import Team18.com.infy.order.dto.OrderDTO;
import Team18.com.infy.order.dto.OrderPlacedDTO;
import Team18.com.infy.order.dto.ProductDTO;
import Team18.com.infy.order.entity.Order;
import Team18.com.infy.order.entity.ProductsOrdered;
import Team18.com.infy.order.exception.OrderMsException;
import Team18.com.infy.order.repository.OrderRepository;
import Team18.com.infy.order.repository.ProductsOrderedRepository;
import Team18.com.infy.order.utility.CustomPK;
import Team18.com.infy.order.utility.OrderStatus;
import Team18.com.infy.order.validator.OrderValidator;

@Service(value = "orderService")
@Transactional
public class OrderServiceImpl implements OrderService {
	
	private static int o;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private ProductsOrderedRepository prodOrderedRepository;
	
	static {
		o=100;
	}

	@Override
	public List<OrderDTO> viewAllOrders() throws OrderMsException {
		Iterable<Order> orders = orderRepository.findAll();
		List<OrderDTO> dtoList = new ArrayList<>();
		orders.forEach(order -> {
			OrderDTO odto = new OrderDTO();
			odto.setOrderId(order.getOrderId());
			odto.setBuyerId(order.getBuyerId());
			odto.setAmount(order.getAmount());
			odto.setAddress(order.getAddress());
			odto.setDate(order.getDate());
			odto.setStatus(order.getStatus());
			dtoList.add(odto);			
		});
		if(dtoList.isEmpty()) throw new OrderMsException("No orders available");
		return dtoList;
	}

	@Override
	public OrderPlacedDTO placeOrder(List<ProductDTO> productList, List<CartDTO> cartList, OrderDTO orderDTO)
			throws OrderMsException {
		Order order = new Order();
		OrderValidator.validateOrder(orderDTO);
		String id = "O" + o++;
		order.setOrderId(id);
		order.setAddress(orderDTO.getAddress());
		order.setBuyerId(cartList.get(0).getBuyerId());
		order.setDate(LocalDate.now());
		order.setStatus(OrderStatus.ORDER_PLACED.toString());	
		order.setAmount(0f);
		List<ProductsOrdered> productsOrdered = new ArrayList<>();
		for(int i = 0; i<cartList.size();i++) {
			OrderValidator.validateStock(cartList.get(i), productList.get(i));			
			order.setAmount(order.getAmount()+(cartList.get(i).getQuantity()*productList.get(i).getPrice()));
			
			ProductsOrdered prodO = new ProductsOrdered();
			prodO.setSellerId(productList.get(i).getSellerId());
			prodO.setPrimaryKeys(new CustomPK(cartList.get(i).getBuyerId(),productList.get(i).getProdId()));
			prodO.setQuantity(cartList.get(i).getQuantity());
			productsOrdered.add(prodO);				
		}		
		prodOrderedRepository.saveAll(productsOrdered);
		orderRepository.save(order);
		OrderPlacedDTO orderPlaced = new OrderPlacedDTO();
		orderPlaced.setBuyerId(order.getBuyerId());
		orderPlaced.setOrderId(order.getOrderId());
		Integer rewardPts = (int) (order.getAmount()/100);		
		orderPlaced.setRewardPoints(rewardPts);
		
		
		return orderPlaced;
	}

	@Override
	public List<OrderDTO> viewOrdersByBuyer(String buyerId) throws OrderMsException {
		List<Order> orders = orderRepository.findByBuyerId(buyerId);
		if(orders.isEmpty()) throw new OrderMsException("No orders available for given BuyerID");
		List<OrderDTO> dtoList = new ArrayList<>();
		orders.forEach(order->{
			OrderDTO odto = new OrderDTO();
			odto.setOrderId(order.getOrderId());
			odto.setBuyerId(order.getBuyerId());
			odto.setAmount(order.getAmount());
			odto.setAddress(order.getAddress());
			odto.setDate(order.getDate());
			odto.setStatus(order.getStatus());
			dtoList.add(odto);
		});
		return dtoList;
	}

	@Override
	public OrderDTO viewOrder(String orderId) throws OrderMsException {
		Optional<Order> optional = orderRepository.findByOrderId(orderId);
		Order order = optional.orElseThrow(()->new OrderMsException("Order does not exist"));
		OrderDTO orderDTO = new OrderDTO();
		orderDTO.setOrderId(order.getOrderId());
		orderDTO.setBuyerId(order.getBuyerId());
		orderDTO.setAmount(order.getAmount());
		orderDTO.setAddress(order.getAddress());
		orderDTO.setDate(order.getDate());
		orderDTO.setStatus(order.getStatus());		
		return orderDTO;
	}
	@Override
	public void updateOrderStatus(String orderId, String status) throws OrderMsException {
		Optional<Order> order = orderRepository.findByOrderId(orderId);
		if (order.isPresent() == true) {
			order.get().setStatus(status);
			orderRepository.save(order.get());
		} else
			throw new OrderMsException("Order not found!!");

	}
	@Override
	public String reOrder(String buyerId, String orderId) throws OrderMsException {
		Optional<Order> optional = orderRepository.findByOrderId(orderId);
		Order order = optional.orElseThrow(()->new OrderMsException("Order does not exist for the given buyer"));
		Order reorder = new Order();
		String id = "O" + o++;
		reorder.setOrderId(id);
		reorder.setBuyerId(order.getBuyerId());
		reorder.setAmount(order.getAmount());
		reorder.setAddress(order.getAddress());
		reorder.setDate(LocalDate.now());
		reorder.setStatus(order.getStatus());
		
		orderRepository.save(reorder);		
		return reorder.getOrderId();
	}

}
