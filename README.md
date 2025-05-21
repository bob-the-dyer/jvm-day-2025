# TODO тут будет название доклада

## Описание

TODO тут будет описание доклада

## Кому может быть интересно, целевая аудитория

TODO

## Мотивация к появлению данного доклада

#### 1

Я делал доклад в 2019 на хайлоде, кому интересно проходите по ссылке.

В докладе сравнивал решения для 3 классических задач многопоточности на "классике" и вертексе, потому что классику
сложная, а в вертексе прикольный API и подход.

У меня остался открытый гештальт - честно измерить производительность решений.

#### 2

Коллеги говорят, установили spring.threads.virtual.enabled=true и все стало быстрее в 2 раза

#### 3

Другие коллеги говорят, установили spring.threads.virtual.enabled=true и ничего не изменилось

#### 4

Другие коллеги говорят, установили spring.threads.virtual.enabled=true и ничего не изменилось

#### 4

Вышли виртуальные потоки

## Ссылка на доклад на хайлоде

https://youtu.be/BpjpPrH_0p0

## Минимум знаний, необходимый но недостаточный для полноценного понимания материала:

- concurrency - synchronized, ReentrantLock, CyclicBarrier, AtomicInteger (CAS)
- virtual threads, structured concurrency - необязательно, но не будут объясняться на пальцах с нуля
- junit 5
- jmh
- vert.x

## Что разбирается в докладе

Феномены, которые хочется продемонстрировать, мифы которые хочется развенчать.

- виртуальные потоки создаются легко в большом количестве: 10К, 100К, 1КК
- switch context виртуальных потоков быстрее платформенных
- blocking (on IO) на виртуальных потоках быстрее
- пининг на synchronized реально починили
- решения на вертекс сравнимы по производительности с "классикой"
  (тут и далее под классикой понимаем java concurrency и инструментарий из java.util.concurrent, включая project loom)
- решения на вертекс проще и нагляднее с точки зрения АПИ чем классика
- synchronized по производительности работает хуже ReentrantLock
- инженерный подход (измеряю-анализирую-синтезирую-измеряю) применим

## Ссылки

https://wiki.openjdk.org/display/loom/Main

https://openjdk.org/jeps/444

https://openjdk.org/jeps/505

https://en.wikipedia.org/wiki/Dining_philosophers_problem

https://spring.io/blog/2022/10/11/embracing-virtual-threads

https://vertx.io/docs/5.0.0/vertx-core/java/#virtual_threads

https://vertx.io/docs/5.0.0/vertx-junit5/java/

## Цитаты для разбора и использования за и против и затравки

#### Loom

> Virtual threads are not faster threads — they do not run code any faster than platform threads. They exist to provide
> scale (higher throughput), not speed (lower latency). There can be many more of them than platform threads, so they
> enable the higher concurrency needed for higher throughput according to Little's Law.

> To put it another way, virtual threads can significantly improve application throughput when
> - The number of concurrent tasks is high (more than a few thousand), and
> - The workload is not CPU-bound, since having many more threads than processor cores cannot improve throughput in that
    case.

> Virtual threads are not cooperative.

> Typically, a virtual thread will unmount when it blocks on I/O or some other blocking operation in the JDK, such as
> BlockingQueue.take(). When the blocking operation is ready to complete (e.g., bytes have been received on a socket),
> it
> submits the virtual thread back to the scheduler, which will mount the virtual thread on a carrier to resume
> execution.

> The vast majority of blocking operations in the JDK will unmount the virtual thread, freeing its carrier and the
> underlying OS thread to take on new work. However, some blocking operations in the JDK do not unmount the virtual
> thread, and thus block both its carrier and the underlying OS thread. This is because of limitations either at the OS
> level (e.g., many filesystem operations) or at the JDK level (e.g., Object.wait())

> There are two scenarios in which a virtual thread cannot be unmounted during blocking operations because it is pinned
> to its carrier:
> - When it executes code inside a synchronized block or method, or
> - When it executes a native method or a foreign function.

> The stacks of virtual threads are stored in Java's garbage-collected heap as stack chunk objects.

> Unlike platform thread stacks, virtual thread stacks are not GC roots, so the references contained in them are not
> traversed in a stop-the-world pause by garbage collectors, such as G1, that perform concurrent heap scanning. This
> also
> means that if a virtual thread is blocked on, e.g., BlockingQueue.take(), and no other thread can obtain a reference
> to
> either the virtual thread or the queue, then the thread can be garbage collected — which is fine, since the virtual
> thread can never be interrupted or unblocked. Of course, the virtual thread will not be garbage collected if it is
> running or if it is blocked and could ever be unblocked.

#### Vert'x

> A virtual thread verticle is just like a standard verticle but it’s executed using virtual threads, rather than using
> an event loop.

> Virtual thread verticles are designed to use an async/await model with Vert.x futures.

## История героя

#### Представляюсь, мотивация, прошлый доклад, для кого, озвучиваю план крупными мазками

#### Беру прошлое решение для философов на ReentrantLock с хайлода и сильно его упрощаю: убираю случайную задержку на кормлении и смотрю что получается

[_001_reentrant_naive](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_001_reentrant_naive)

Почему философов: хорошо масштабируются. Ну и восток наше все!

Начал с ReentrantLock, погонял на платформенных потоках и на виртуальных;

На глаз может показаться что виртуальные потоки показывают худшее время, но я решил верить только бенчмаркам и тестам;

Но чтобы написать тест или бенчмарк нам не подойдет решение, которое бежит указанное время, надо сделать pivot

#### Pivot для ReentrantLock + structured concurrency

[_002_reentrant_pivot](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_002_reentrant_pivot)

Переписываю логику, чтобы иметь возможность мерять время выполнения с указанным числом философов до того момента когда
один из философов совершит указанное число кормлений;

И для пивота, оказывается, очень подходит structured concurrency: оно само остановит выполнение по достижении одним из
философов указанного числа кормлений.

Вот теперь можно писать тесты и бенчмарки, но прежде ...

#### Решение для synchronized + Pivot

[_003_synchronized_pivot](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_003_synchronized_pivot)

Беру прошлое решение для философов с synchronized с хайлода и сильно его упрощаю и сразу делаю pivot

#### Решение для synchronized + Pivot

[_004_junit5_tests_jdk](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_004_junit5_tests_jdk)

Теперь можно написать юнит тесты.

Нам хорошо подойдет @RepeatedTest(200), уже можно будет сравнивать платформенные потоки с виртуальными и ReentrantLock с
synchronized.

На 200 повторов с 1К философов и 10К кормлений в пике результаты такие:

[Test Results - _004_junit5_tests_jdk_in_jvm-day-2025.html](Test%20Results%20-%20_004_junit5_tests_jdk_in_jvm-day-2025.html)

По тестам можно решить что виртуальные треды на порядок рвут платформенные, при этом под синхронайзом они быстрее
реентрант лока. Это мы проверим на бенчмарках! Но позже!

#### Нюансы с компилятором, рантаймом и железом

На разных jdk + jvm были немного разные результаты по погрешностям, разбор почему так происходит выходит за рамки
доклада; средние значения если и различались то несущественно.

Проверки делались на:

- openjdk-24.0.1
- liberica-full-24.0.1
- liberica-24.0.1
- temurin-24.0.1

В дальнейшем все компилировалось и бежало на openjdk-24.0.1, конечные результаты приведены также для нее.

Также все эксперименты кроме скалирования приведены для 1К философов и до 10К кормлений в пике для первого добежавшего
философа.

Все тестировалось на моей локельной тачке macOS 15.5: 2,6 GHz 6-Core Intel Core i7 16 GB, диск APPLE SSD AP0512N

На моей тачке для пользовательского жава процесса доступно примерно 4060+ платформенных потоков.
При переборе летит, как и ожидалось:

> [0.701s][warning][os,thread] Failed to start thread "Unknown thread" - pthread_create failed (EAGAIN) for attributes:
> stacksize: 1024k, guardsize: 4k, detached.
> [0.702s][warning][os,thread] Failed to start the native thread for java.lang.Thread "Thread-4065"
> Exception in thread "main" java.lang.OutOfMemoryError: unable to create native thread: possibly out of memory or
> process/resource limits reached

Конкретно сколько jvm ползует покажет Thread.activeCount(), но не точно, а примерно. Для виртуальных тредов под
дебаггером вообще бывает виден один активный;

И не забываем включить опцию для компилятора и VM --enable-preview чтоб заработало structured concurrency, релиз
планируется в 25 жаве - ну-ну...

#### Нюансы с методологией тестирования и замерами

Изначально я задумывал разделить в тестах и бенчмарках логику инициализации философов и их прогоны, чтобы время
инициализации не считалось и не входило в конечный замер. Однако это сильно усложнило логику сброса состояния философов
между прогонами и в итоге я решил от этого отказаться. Абсолютные числа получились больше, но они ничего не значат сами
по себе, важен только порядок и отношения.

Эмпирически было установлено что 3 разогревочных прогонов почти всегда хватает чтобы боевые замеры ложились кучнее

Макс Размер хипа для форкнутого бенчмарка в 4Гб хватало чтобы не ловить OOM в кишках вертекса типа такого на небольших
кол-вах филосовов и кормлений:

> Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "
> vertx-blocked-thread-checker"

Однако на больших кол-вах процессы просто замирали. Доказывать причину не пошел, есть стойкое ощущение, что это затык на
внутренней шине - кол-во подписок и внутренних событий штормило и убивало полезную логику.

Вертекс в тестах и бенчмарках по-честному стопается, это занимает некоторое время, но убирает ругань в логах вертекса на
жесткое выключение под работающими вертиклами и честно стопает процесс, в противном случае форкнутый процесс висит
вечно, jps в помощь:

> JMH had finished, but forked VM did not exit, are there stray running threads? Waiting 9 seconds more...

Логирование для вертекса было отключено, чтобы не тратить время на вывод в консоль.

#### Пишем первые бенчмарки чтобы уже на что-то опираться - jmh

[_005_jmh_benchmarks_jdk](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_005_jmh_benchmarks_jdk)

Сначала казалось, что если обогатить юнит-тест правильными аннотациями и дописать мейн метод то юнит-тест легко
заработает и как бенчмарк, и как юнит-тест.

Но оказалось, что сгенеренный jmh код тоже воспринимается как тесты ибо наследуется от тестов и в итоге запускается как
тест. Поэтому решил не экономить на копи-пасте.

Используем черную дыру для исключения эффектов оптимизаций.

Benchmark Mode Cnt Score Error Units
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 2.088 ± 0.122 ms/op
SynchronizedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads avgt 7 2.235 ± 0.100 ms/op
SynchronizedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads avgt 7 114.038 ± 16.327
ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads avgt 7 137.708 ± 13.519
ms/op
Benchmark Mode Cnt Score Error Units

Видно, что виртуальные потоки справляются лучше платформенных на 2 порядка, а synchronized и ReentrantLock сравнимы в
пределах погрешности

#### Методология проверки пининга синхронайза на блокирующем вызове

Выяснили, что с виртуальными потоками мы можем держать огромное кол-во некооперирующих потоков - это хорошо для
промышленных
стандартных задач, именно обработка запросов на вебсервере.

Но наш философ не покажет нам пининг, так как внутри кормления нет блокирующего метода. Так давайте же сделаем!

Какие варианты устроить настоящую блокировку:

- поход по сети по урлу (непредсказуемо из-за самой сети или упремся в сервер)
- вызов блокирующего вызова на блокующей очереди (сложно сделать непротиворечивую логику)
- чтение файла (не идеально, но предсказуемей на SSD и на последовательном чтении)

А сравнивать по производительности будем с:

- активным ожиданием (читай итерация в цикле по времени) на время сопоставимое со временем последовательного чтения из
  файла на SSD
- слипами потока на время сопоставимое со временем последовательного чтения из файла на SSD

Модифицируем всех философов, чтобы они принимали в себя рабочую нагрузку и поочередно замеряем.

Давайте глянем на кусочек из модного систем дезайн интервью:
https://habrastorage.org/r/w1560/getpro/habr/upload_files/20b/769/22f/20b76922f1069403081d4b0818d24970.png

Тут находим опорное значение для чтения, опытным путем приходим к "правильному" размеру файла - 16КB: не слишком долго и
не слишком мало. Ожидаемое время - 16К nanosec

#### Модифицируем философов так, чтобы они принимали в себя рабочую нагрузку

#### Модифицируем бенчмарки так, чтобы они отдавали в философов рабочую нагрузку и тестируем в 4 вариантах

[_006_jmh_benchmarks_jdk_noop](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_006_jmh_benchmarks_jdk_noop)

Benchmark Mode Cnt Score Error Units
NoopPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 1.898 ± 0.048 ms/op
NoopPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads avgt 7 1.927 ± 0.058 ms/op
NoopPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads avgt 7 96.351 ± 0.709 ms/op
NoopPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads avgt 7 129.329 ± 15.365 ms/op

Виртуальные потоки на 2 порядка лучше, а synchronized и ReentrantLock сравнимы в пределах погрешности, тут все бьется с
предыдущими измерениями.

Будем аккуратно сравнивать между собой следующие 3 бенчмарки, т к можем промахнуться с оценкой операции реального чтения
с SSD.

[_007_jmh_benchmarks_jdk_sleeping](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_007_jmh_benchmarks_jdk_sleeping)

Benchmark Mode Cnt Score Error Units
SleepingPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_platform_threads avgt 7 619.487 ± 57.005
ms/op
SleepingPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_platform_threads avgt 7 695.648 ± 47.757
ms/op
SleepingPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_virtual_threads avgt 7 743.421 ± 111.426
ms/op
SleepingPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_virtual_threads avgt 7 757.324 ± 85.793
ms/op

Тут нет однозначности, я бы сказал, что все сравнимо в пределах погрешностей. Если судить только по средним, то
как-будто платформенные потоки лучше засыпают и просыпаются, но с чего бы это на самом деле?...

[_008_jmh_benchmarks_jdk_blocking_reading](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_008_jmh_benchmarks_jdk_blocking_reading)

Benchmark Mode Cnt Score Error Units
BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_platform_threads avgt 7
585.240 ± 94.912 ms/op
BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_platform_threads avgt 7
906.057 ± 107.330 ms/op
BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads avgt 7
3610.834 ± 744.468 ms/op
BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_virtual_threads avgt 7
17666.041 ± 5157.029 ms/op

Как я вижу эти результаты?

- виртуальные потоки на порядок или 2 хуже платформенных даже с учетом погрешности
- последнюю строчку я бы интерпретировал либо как пининг виртуального потока на блокирующем апи при вызове внутри
  synchronized, либо как
  провокацию или багу! Я смотрю на такое же чтение в synchronized на платформенном потоке и не понимаю почему
  виртуальному настолько плохо.
- самое вероятное объяснение - методология плоха тем, что смешивает измерение производительности при борьбе за
  палочки/мониторы и блокирующее чтение ИЛИ вообще чтение файла было не самой лучшей идеей
- лучшие результаты по блокирующему чтению под мониторами по времени похожи на слип, но хуже в 2,5 раза активного
  ожидания - активно ждать дешевле слипа в итоге?

[_009_jmh_benchmarks_jdk_active_waiting](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_009_jmh_benchmarks_jdk_active_waiting)

Benchmark Mode Cnt Score Error Units
ActiveWaitingPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_virtual_threads avgt 7 170.812 ±
7.355 ms/op
ActiveWaitingPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_virtual_threads avgt 7 178.575 ±
16.057 ms/op
ActiveWaitingPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_platform_threads avgt 7 303.367 ±
12.389 ms/op
ActiveWaitingPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_platform_threads avgt 7
416.446 ± 92.891 ms/op

Активное ожидание показывает лучшие результаты на виртуальных потоках в 1,5 раза, остальное в пределах погрешностей.

Косвенно это может означать, что если вам не повезло с драйвером и он не уходит в IO, то на виртуальных потоках под
нагрузкой все равно может получиться профит за счет более быстрого переключения контекста, НО ТОЛЬКО если нет затыков на
самом драйвере.

Другой осторожный вывод подтверждается: активно ждать эффективнее слипа

#### В этом месте по идее надо уходить и строить специальный тест без захвата 2-х палок чисто на блокирующее чтение из-под монитора.

#### Вместо этого мы слегка уходим в сторону и возвращаемся к акторной модели и вертексу

#### Минимальный ликбез по вертексу+акторной модели

- Мультиреактор
- На моем компе 12 логических ядер - в вертексе будет мультиреактор на 12 потоков
- Виды вертиклов, новый тип
- Особенность кода для виртуальных вертиклов
- Ликбез Очереди+топики+sharedata - то что надо понять для примеров

#### Реализуем философов на вертексе сразу так, чтобы затаскивать в тесты и в бенчмарки (pivot)

[_010_vertx_pivot](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_010_vertx_pivot)

Код получился чище - без классической конкарренси совсем!

[_011_junit5_tests_vertx](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_011_junit5_tests_vertx)

Затащил тестовую библиотеку для вертекса. Удобно.

[Test Results - _011_junit5_tests_vertx_in_jvm-day-2025.html](Test%20Results%20-%20_011_junit5_tests_vertx_in_jvm-day-2025.html)

По тестам виртуальные вертиклы рвут те что в event-loop в 1,5 раза, посмотрим как в бенчмарке!

[_012_jmh_benchmarks_vertx_noop](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_012_jmh_benchmarks_vertx_noop)
[_013_jmh_benchmarks_vertx_blocking_reading](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_013_jmh_benchmarks_vertx_blocking_reading)
[_014_jmh_benchmarks_vertx_active_waiting](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_014_jmh_benchmarks_vertx_active_waiting)
[_015_jmh_benchmarks_vertx_sleeping](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_015_jmh_benchmarks_vertx_sleeping)

Тут следующие допущения:

- ивент луп блокировать нельзя, блокирующее чтение и слипы в вертексе запрещены - это надо делать либо в рабочих
  вертиклах либо в виртуальных, т е существующие стратегии constructXXXEating() по-хорошему не подойдут для event loop
  вертиклов - НО МЫ НАРУШИМ!!
- в вертексе есть асинхронное/неблокирующее чтение файла - надо решить насколько честно было использовать его - НО МЫ НЕ
  БУДЕМ ЕГО ИСПОЛЬЗОВАТЬ!!
- также можно через таймер красиво решить активное ожидание - НО МЫ НЕ БУДЕМ ЭТОГО ДЕЛАТЬ!!

#### Объединенные бенчмарки по всем типам

[_099_jmh_benchmarks_united](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_099_jmh_benchmarks_united)

Для удобства проведения всех измерений за один прогон объединил все бенчмарки в один

Benchmark Mode Cnt Score Error Units
UnitedPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 2.093 ± 0.220 ms/op
UnitedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads avgt 7 2.397 ± 0.164 ms/op
UnitedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads avgt 7 109.763 ± 16.223 ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads avgt 7 131.368 ± 16.621 ms/op
UnitedPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_virtual_threads avgt 7 171.617 ± 4.052
ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_virtual_threads avgt 7 179.902 ± 29.410
ms/op
UnitedPhilosophersBenchmark.test_virtual_noop_verticle_philosophers avgt 7 206.376 ± 12.699 ms/op
UnitedPhilosophersBenchmark.test_verticle_noop_philosophers avgt 7 288.937 ± 26.813 ms/op
UnitedPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_platform_threads avgt 7 325.382 ± 34.030
ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_platform_threads avgt 7 439.129 ±
111.866 ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_platform_threads avgt 7 548.097 ±
122.230 ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_platform_threads avgt 7 611.358 ± 102.814
ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_virtual_threads avgt 7 649.195 ± 68.691 ms/op
UnitedPhilosophersBenchmark.test_active_waiting_verticle_philosophers avgt 7 678.343 ± 22.580 ms/op
UnitedPhilosophersBenchmark.test_virtual_active_waiting_verticle_philosophers avgt 7 718.618 ± 59.118 ms/op
UnitedPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_platform_threads avgt 7 767.795 ± 89.405 ms/op
UnitedPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_virtual_threads avgt 7 845.711 ± 78.225 ms/op
UnitedPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_platform_threads avgt 7 929.553 ±
73.032 ms/op
UnitedPhilosophersBenchmark.test_blocking_reading_verticle_philosophers avgt 7 2820.554 ± 147.504 ms/op
UnitedPhilosophersBenchmark.test_virtual_blocking_reading_verticle_philosophers avgt 7 3229.174 ± 106.756 ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads avgt 7 3578.460 ±
558.093 ms/op
UnitedPhilosophersBenchmark.test_sleeping_verticle_philosophers avgt 7 3596.082 ± 412.365 ms/op
UnitedPhilosophersBenchmark.test_virtual_sleeping_verticle_philosophers avgt 7 3953.206 ± 232.746 ms/op
UnitedPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_virtual_threads avgt 7 16583.318 ±
4071.447 ms/op

Комментарии

- быстрее всего в классике либо ничего не делать, либо активно ждать. и где-то рядом болтается ничего не делать в
  вертексе.
- подтверждается на порядок просадка в производительности в блокирующем вызове виртуального потока под synchronized, что
  особенно странно т к относительно такого же для платформенного варианта имеем на порядок лучше показатель хотя там УЖЕ
  и был
  платформенный поток

#### А теперь попробуем поскалировать наших философов по 2м измерениям: кол-ву философов и кол-ву кормлений

[_999_jmh_benchmarks_scaled](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_999_jmh_benchmarks_scaled)

В скалированный сводный бенчмарк не входят решения на вертексе с большими числами философов и кормлений;
Это потому что такие бенчмарки залипают! Не вдаваясь в подробности, кажется, что шторм событий от подписок и отписок
просто выносит всю полезную нагрузку. Ну а что с десятками тысяч вертиклов?

ScaledPhilosophersBenchmark._020_test_0010K_0010K_synchronized_noop_philosophers_with_virtual_threads avgt 7 13.453 ±
2.037 ms/op
ScaledPhilosophersBenchmark._010_test_0010K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 13.790 ±
0.852 ms/op
UnitedPhilosophersBenchmark._____test_0001K_0010K_test_virtual_noop_verticle_philosophers avgt 5 204.009 ± 20.128 ms/op
UnitedPhilosophersBenchmark._____test_0001K_0010_verticle_noop_philosophers avgt 5 287.172 ± 31.099 ms/op
ScaledPhilosophersBenchmark._040_test_0010K_0010K_noop_verticle_philosophers avgt 5 527.384 ± 330.028 ms/op
ScaledPhilosophersBenchmark._030_test_0010K_0010K_virtual_noop_verticle_philosophers avgt 5 1028.263 ± 1976.565 ms/op

Вертекс хуже на порядок классики! Погрешности ужасны, вообще непонятно что намеряли?! Оснований доверять таким числам
нет! Оставляем только измерения для классики.

UnitedPhilosophersBenchmark._____test_0001K_0010K_reentrant_lock_philosophers_with_virtual_thread avgt 5 1.952 ± 0.188
ms/op
UnitedPhilosophersBenchmark._____test_0001K_0010K_test_synchronized_philosophers_with_virtual_threads avgt 5 2.184 ±
0.439 ms/op
ScaledPhilosophersBenchmark._020_test_0010K_0010K_synchronized_noop_philosophers_with_virtual_threads avgt 7 13.453 ±
2.037 ms/op
ScaledPhilosophersBenchmark._010_test_0010K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 13.790 ±
0.852 ms/op
ScaledPhilosophersBenchmark._100_test_0010K_0100K_synchronized_noop_philosophers_with_virtual_threads avgt 7 20.490 ±
0.973 ms/op
ScaledPhilosophersBenchmark._090_test_0010K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 21.520 ±
2.153 ms/op
ScaledPhilosophersBenchmark._220_test_0010K_1000K_synchronized_noop_philosophers_with_virtual_threads avgt 7 70.700 ±
1.221 ms/op
ScaledPhilosophersBenchmark._210_test_0010K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 72.035 ±
1.424 ms/op
ScaledPhilosophersBenchmark._060_test_0100K_0010K_synchronized_noop_philosophers_with_virtual_threads avgt 7 276.019 ±
92.038 ms/op
ScaledPhilosophersBenchmark._050_test_0100K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 284.643 ±
35.604 ms/op
ScaledPhilosophersBenchmark._130_test_0100K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 297.571 ±
37.570 ms/op
ScaledPhilosophersBenchmark._140_test_0100K_0100K_synchronized_noop_philosophers_with_virtual_threads avgt 7 255.395 ±
102.263 ms/op
ScaledPhilosophersBenchmark._142_test_0100K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7 351.140 ±
102.412 ms/op
ScaledPhilosophersBenchmark._144_test_0100K_1000K_synchronized_noop_philosophers_with_virtual_threads avgt 7 382.512 ±
60.712 ms/op
ScaledPhilosophersBenchmark._180_test_1000K_0010K_synchronized_noop_philosophers_with_virtual_threads avgt 7 3847.501 ±
197.755 ms/op
ScaledPhilosophersBenchmark._170_test_1000K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7
4307.880 ± 402.627 ms/op
ScaledPhilosophersBenchmark._182_test_1000K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7
3993.247 ± 530.418 ms/op
ScaledPhilosophersBenchmark._184_test_1000K_0100K_synchronized_noop_philosophers_with_virtual_threads avgt 7 4089.308 ±
576.735 ms/op
ScaledPhilosophersBenchmark._250_test_1000K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads avgt 7
3931.608 ± 460.112 ms/op
ScaledPhilosophersBenchmark._260_test_1000K_1000K_synchronized_noop_philosophers_with_virtual_threads avgt 7 3792.618 ±
352.711 ms/op

В классике очень логичная лесенка по порядкам философов: увеличиваем кол-во философов на порядок - время увеличивается
на порядок, т е предсказуемое, линейное масштабирование.

В классике целевое кол-во кормлений незначительно влияет на время выполнения - не на порядки как с ростом философов:
небольшая, линейная и даже инога в пределах погрешности.

synchronized vs reentrant_lock сравнимы с точностью до погрешности

Так что же получается, классика прям молодец и инженеры которые контрибьютят в жаву не зря едят свой хлеб с Маслоу??!
Да, кроме потенциально непофикшеного бага с пинингом.

## Итоги выводы

- измерения получается инструментально выполнить и на классике, и на вертексе. На классике проще, на вертексе уже есть
  утилки, но для бенчмарка вертекса приходится призывать классику "к барьеру"
- классика масштабируется прогнозируемо, линейно
- классика сложнее - требует более высокого уровня квалификации
- вертекс/акторная модель проще как апи и как концепт, пакета конкарренси нет совсем
- Structure Concurrency уже работает в превью, стало проще закрывать весь пул задач
- синхронайзд против реентратн лок - в основном различий не видно, все в пределах погрешности
- пининг - смотря под каким углом смотреть, возможно да, пининг остался (в опенждк 24,0,1), но возможно методология
  нехорошая (надо выделенный тест)
- блокирующий код vs активное ожидание vs слип - лучше активно ждать чем спать, оценка по времени блокирующего чтения
  может быть неточной, не оцениваем.
- что не так с вертексом на масштбировании?? Вертекс не готов к такому жесткому использованию локально, внутренняя
  механика мешает, а не помогает
- стал бы я прагматично голосовать за вертекс в данном контексте в 2019 - да (даешь новый проект на новой технологии, не
  спринг, команда воодушевлена, риски все мои, комьюнити маленькое, спека), задача утилизации цпу решалась 100%, равно
  как и реактивщиной/асинхронщиной
- стал бы я прагматично голосовать за вертекс в данном контексте в 2025 - нет - зачем, есть же виртуальное потоки!

Чекнем по пунктам удалось ли продемонстрировать заявленные феномены и развенчать мифы:

- виртуальные потоки создаются легко в большом количестве: 10К, 100К, 1КК - ДА, создаются и еще как
- switch context виртуальных потоков быстрее платформенных - ДА, быстрее однозначно
- blocking (on IO) на виртуальных потоках быстрее - НЕТ, мы этого не померили
- пининг на synchronized реально починили - НЕТ, по моим замерам не починили
- решения на вертекс сравнимы по производительности с "классикой" - НЕТ, так заявить нельзя
- решения на вертекс проще и нагляднее с точки зрения АПИ чем классика - ДА, это точно
- synchronized по производительности работает хуже ReentrantLock - НЕТ, сравнимы друг с другом в пределах погрешности
- инженерный подход (измеряю-анализирую-синтезирую-измеряю) применим - ДА, вполне, этот доклад как раз про это
