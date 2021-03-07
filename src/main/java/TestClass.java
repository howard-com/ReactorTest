import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class TestClass {

	// 创建Flux Mono的若干方法
	public void test1() {
		Mono.just("Mono.just 发送简单对象").subscribe(System.out::println);

		System.out.println();
		Flux.create((v) -> {
			v.next("Flux.create 发送对象1");
			v.next("Flux.create 发送对象2");
			v.next("Flux.create 发送对象3");
		}).subscribe(System.out::println);

		System.out.println();
		Flux.generate(sink -> {
			sink.next("Flux.generate 发送单一对象");
			sink.complete();
		}).subscribe(System.out::println);

		System.out.println();
		Flux.generate(ArrayList::new, (list, sink) -> {
			int cnt = list.size() + 1;
			String value = "Flux.generate 循环发送多个对象" + cnt;
			list.add(value);
			sink.next(value);
			if (list.size() == 10) {
				sink.complete();
			}
			return list;
		}).subscribe(System.out::println);
	}

	// 测试Mono对象的创建时机
	public void test2() {
		System.out.println("普通创建方式:");
		String res1 = createNewStr();
		System.out.println("普通方式创建完成!");
		System.out.println("普通方式结果是：" + res1);

		new Scanner(System.in).nextLine();

		System.out.println("Mono创建方式:");
		Mono<String> res2 = Mono.fromSupplier(() -> createNewStr());
		System.out.println("Mono创建方式完成！");
		res2.subscribeOn(Schedulers.parallel()).subscribe(v -> System.out.println("Mono方式结果是:" + v));
		this.fakeLoad(6000);
		; // 故意等一下确保订阅方法被执行
	}

	public String createNewStr() {
		System.out.println("开始创建新的字符串...");
		fakeLoad(3000);
		return "这是一个新的字符串";
	}

	private String getThreadName() {
		return Thread.currentThread().getStackTrace().getClass().getName();
	}

	// 测试Flux create方法调用的时机
	public void test3() {
		Flux<String> flux = Flux.create(v -> {
			System.out.println("开始创建数据");
			v.next("aaa");
			v.next("bbb");
			v.next("ccc");
			v.complete();
		});
		System.out.println("Flux对象创建完成");
		flux.subscribe(System.out::println);
	}

	// 测试异步的创建以及消费数据
	public void test4() {
		Flux.create((sink) -> {
			createObjAsynch(sink);
		}).subscribe(res -> {
			consumeObjAsynch((String) res);
		});
	}

	// 异步创建对象
	private void createObjAsynch(FluxSink sink) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			int i = 1;

			@Override
			public void run() {
				String objName = "对象－" + i++;
				System.out.println(">>>创建对象 " + objName);
				sink.next(objName);
				if (i > 10) {
					this.cancel();
				}
			}
		}, 0, 1000);
	}

	// 异步处理对象
	private void consumeObjAsynch(String input) {
		new Thread(() -> {
			// System.out.println("---处理对象 " + input + " 线程：" +
			// Thread.currentThread().getName());
			int wait = new Random().nextInt(10000);
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("                                              <<<处理对象 " + input);
		}).start();
	}

	// 测试Parallel方式创建Flux
	public void test5() {
		Flux.create(v -> {
			System.out.println("开始创建数据");
			for (int i = 1; i <= 100; i++) {
				v.next("对象" + i);
			}
			v.complete();
		}).parallel(10).runOn(Schedulers.parallel()).subscribe(res -> {
			System.out.println(res + " " + Thread.currentThread().getName());
		});
		this.fakeLoad(1000);
	}

	// 测试Mono对象来进行流水线工作
	public void test6() {
		Mono.just("AAA").flatMap(v -> doStep1(v)).flatMap(v -> doStep2(v)).flatMap(v -> doStep3(v))
				.subscribe(System.out::println);
	}

	private Mono<String> doStep1(String input) {
		return Mono.just(input + " >>>增加内容1");
	}

	private Mono<String> doStep2(String input) {
		return Mono.just(input + " >>>增加内容2");
	}

	private Mono<String> doStep3(String input) {
		return Mono.just(input + " >>>增加内容3");
	}

	// 测试切换线程
	public void test7() {
		say("你好").subscribe();
		fakeLoad(100);
	}

	private Mono<String> say(String name) {
		return Mono.just(name)
				// .publishOn(Schedulers.parallel())
				.map(t -> {
					try {
						return hello(t);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return t;
				});
	}

	private String hello(String name) throws InterruptedException {
		// Thread.sleep(100);
		String result = String.format("hello %s, current-thread is [%s]", name, Thread.currentThread().getName());
		System.out.println(result);
		return result;
	}

	public void test8() {
		String[] input = { "1234", "5678" };

		try {
			flatMap(input);
			flatMapSequential(input);
			flatMapIterable(input);
			concatMap(input);
			concatMapIterable(input);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void flatMap(String[] input) throws InterruptedException {
		System.out.println("flatMap:");
		Flux.just(input).flatMap(i -> Flux.fromArray(i.split("")).delayElements(Duration.ofMillis(10)))
				.subscribe(i -> System.out.print("->" + i));
		Thread.sleep(100);
		System.out.println("");
	}

	private void flatMapSequential(String[] input) throws InterruptedException {
		System.out.println("flatMapSequential:");
		Flux.just(input).flatMapSequential(i -> Flux.fromArray(i.split("")).delayElements(Duration.ofMillis(10)))
				.subscribe(i -> System.out.print("->" + i));
		Thread.sleep(100);
		System.out.println("");
	}

	public void flatMapIterable(String[] input) {
		System.out.println("flatMapIterable:");
		Flux.just(input).flatMapIterable(i -> Arrays.asList(i.split(""))).subscribe(i -> System.out.print("->" + i));
		System.out.println("");
	}

	private void concatMap(String[] input) throws InterruptedException {
		System.out.println("concatMap:");
		Flux.just(input).concatMap(i -> Flux.fromArray(i.split("")).delayElements(Duration.ofMillis(10)))
				.subscribe(i -> System.out.print("->" + i));
		Thread.sleep(110);
		System.out.println("");
	}

	private void concatMapIterable(String[] input) {
		System.out.println("concatMapIterable:");
		Flux.just(input).concatMapIterable(i -> Arrays.asList(i.split(""))).subscribe(i -> System.out.print("->" + i));
		System.out.println("");
	}

	// 测试sort
	public void test9() {

		Student st1 = new Student("张三", 90);
		Student st2 = new Student("李四", 91);
		Student st3 = new Student("王二", 87);
		Student st4 = new Student("李逵", 97);
		Student st5 = new Student("宋江", 85);
		List<Student> list1 = new ArrayList<Student>();
		list1.add(st1);
		list1.add(st2);
		list1.add(st5);
		List<Student> list2 = new ArrayList<Student>();
		list2.add(st3);
		list2.add(st4);
		Teacher t1 = new Teacher();
		t1.setStudents(list1);
		Teacher t2 = new Teacher();
		t2.setStudents(list2);
		List<Teacher> teachers = new ArrayList<Teacher>();
		teachers.add(t1);
		teachers.add(t2);

		Flux.fromIterable(teachers).flatMap(t -> {
			Flux<Student> r = Flux.fromIterable(t.getStudents());
			System.out.println("开始生成学生信息");
			fakeLoad(2000);
			return r;
		}).sort((s1, s2) -> s1.getScore() - s2.getScore()).subscribe(s -> {
			System.out.println(s.name + " - " + s.score);
		});

		fakeLoad(100);
		return;
	}

	public void test10() {
		// 创建一颗含有100个苹果的苹果树
		Tree appleTree = new Tree("Tree-1");

		Apple[] freshApples = new Apple[100];
		for (int i = 1; i <= 100; i++) {
			freshApples[i - 1] = new Apple(appleTree.name + "-" + "Apple-" + i);
		}
		appleTree.apples = freshApples;

		AtomicLong countJ = new AtomicLong(); // 为了给果汁编号设置一个计数器
		
		// 流水线开始
		Mono.just(appleTree).flatMapIterable(t -> {
			// 1.将果树上的苹果取出，每个苹果进行下一步操作。
			System.out.println("从果树{" + appleTree.name + "}上摘到" + t.apples.length + "颗苹果");
			return Arrays.asList(t.apples);
		}).map(a -> {
			// 2.每个苹果都变成一份果汁，然后进行下一步操作。
			System.out.println("生成一份苹果汁{" + a.name + "-Juice" + (countJ.get() + 1) + "}");
			return new Juice(a.name + "-Juice" + countJ.incrementAndGet());
		}).buffer(3).flatMap(juiceList -> {
			// 3.每三份果汁装成一瓶饮料，然后进行下一步操作。不足三份的部分舍去。
			if (juiceList.size() < 3) {
				//throw new IllegalStateException("xxx");
				System.out.println("剩余" + juiceList.size() + "份饮料，无法装满一瓶。");
				return Mono.empty();
			}

			StringBuffer drinkName = new StringBuffer();
			Drink drink = null;
			for (Juice juice : juiceList) {
				if (drinkName.length() > 0)
					drinkName.append("+++");
				drinkName.append(juice.name);
			}
			if (drinkName.length() > 0) {
				drink = new Drink(drinkName.toString());
				System.out.println("灌装一瓶饮料{" + drink.name + "}");
			}
			
			return Mono.just(drink); // 生成一瓶饮料。
		}).collectList() // 将瓶装饮料全部收集起来一起处理
		// 4.将瓶装饮料装箱
		.map(drinks -> new Product("苹果汁产品批次", drinks.toArray(new Drink[drinks.size()]))) 
		.subscribe(p -> {
			System.out.println("饮料生产完毕！");
			System.out.println("得到饮料" + p.drinkList.length + "瓶");
		}, error -> {
			System.err.println("CAUGHT " + error);
		});
	}

	// 测试各种过年不同写法
	public void test99() {
		Consumer<FluxSink<String>> c1 = (v) -> {
			v.next("create1");
			v.next("create2");
			v.next("create3");
			v.complete();
		};

		Flux.create(c1).subscribe(System.out::println);

		Consumer<String> c2;
		c2 = (v) -> {
			System.out.println(v);
		};

		c2.accept("Im c2");

		Flux.generate(t -> {
			t.next("generate1");
			// 注意generate中next只能调用1次
			t.complete();
		}).subscribe(System.out::println);
	}

	public void fakeLoad(int wait) {
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

class Student {
	String name;
	int score;

	public int getScore() {
		return this.score;
	}

	public Student(String name, int score) {
		this.name = name;
		this.score = score;

	}
}

class Teacher {
	List<Student> students;

	public void setStudents(List<Student> s) {
		this.students = s;
	}

	public List<Student> getStudents() {
		return this.students;
	}
}

class TSEntity {
	public String id;
	public String name;
}

//-----------------Drink process--------------------
class Tree {
	Apple[] apples;
	String name;

	public Tree(String name) {
		this.name = name;
	}
}

class Apple {
	String name;

	public Apple(String name) {
		this.name = name;
	}
}

class Juice {
	String name;

	public Juice(String name) {
		this.name = name;
	}
}

class Drink {
	String name;

	public Drink(String name) {
		this.name = name;
	}
}

class Product {
	String name;
	Drink[] drinkList;

	public Product(String name, Drink[] drinks) {
		this.name = name;
		this.drinkList = drinks;
	}
}