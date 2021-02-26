import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
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
		}).parallel(10)
		.runOn(Schedulers.parallel())
		.subscribe(res -> {
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

	// 测试各种过年不同写法
	public void test9() {
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
