package com.techprimers.jpa.springjpahibernateexample;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import com.techprimers.jpa.springjpahibernateexample.model.Users;

public class SpringJpaHibernateExampleApplicationTests {

	@Test
	public void testUserGettersAndSetters() {
		Users user = new Users();
		user.setName("William Smith");
		user.setId(15);
		user.setSalary(15000);

		assertEquals("William Smith", user.getName());
		assertEquals(Integer.valueOf(15), user.getId());
		assertEquals(Integer.valueOf(15000), user.getSalary());
	}
}
