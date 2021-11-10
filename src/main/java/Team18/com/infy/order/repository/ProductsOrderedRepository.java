package Team18.com.infy.order.repository;

import org.springframework.data.repository.CrudRepository;

import Team18.com.infy.order.entity.ProductsOrdered;
import Team18.com.infy.order.utility.CustomPK;

public interface ProductsOrderedRepository extends CrudRepository<ProductsOrdered, CustomPK>{

}
