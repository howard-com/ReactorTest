import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Random;

import javax.management.ReflectionException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MainTest {

	public static void main(String[] args) throws InterruptedException {
//		startTest("test1");
//		startTest("test2");
//		startTest("test3");
//		startTest("test4");
		startTest("test5");
//		startTest("test6");
//		startTest("test7");
		
	}

	static void startTest (String methodName){
		try {
			TestClass testClass = new TestClass();
			Class<? extends TestClass> classType = testClass.getClass();
			System.out.println("------------测试 " + methodName + " 开始-----------------");
			classType.getMethod(methodName, null).invoke(testClass, null);
			System.out.println("------------测试 " + methodName + " 結束-----------------");
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			e.printStackTrace();
		}
	}
	
}
