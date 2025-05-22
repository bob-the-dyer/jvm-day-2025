# --TODO-- тут будет название доклада

Рабочие варианты:

- Виртуальные войны звездных философов
- Виртуальные войны: vert.x против классической многопоточности
- Виртуальные войны: vert.x против классической многопоточности, эпизод №2
- Виртуальные войны: vert.x классическая многопоточность против vert.x
- Виртуальные войны: project loom vs vert.x
- Виртуальные войны философов: project loom vs vert.x
- Философские войны: project loom vs vert.x
- Философские войны: loom vs vert.x
- Битва философов: loom vs vert.x
- Битва философов: loom против vert.x
- Виртуальные войны: loom vs vert.x

## Описание

Данный доклад является продолжением моего доклада "vert.x против классической многопоточности в JVM", который был
представлен на SPb HighLoad++ в 2019г.
Цель этого доклада - пролить свет не некоторые заблуждения, утверждения и мифы, связанные с виртуальными
потоками, применив инженерный подход, основанный на измерениях.

В ходе доклада будут приведены измерения производительности различных решений одной из классических задач
многопоточности - "Обедающие философы". Варианты решений будут включать в себя реализации не виртуальных и на
платформенных потоках, а также в нескольких вариациях:

- на ReentrantLock
- на synchronized
- на vert'x

Измеряя и анализируя результаты слушатель, в частности, узнает:

- починили ли пиннинг (pinning) виртуальных потоков в synchronized секциях
- быстрее ли switch context на виртуальных потоках чем на платформенных и на сколько
- сравнимы ли решения на vert.x по производительности с "классикой"
  (тут и далее под классикой понимаем java concurrency и инструментарий из java.util.concurrent, включая project loom)
- что выгоднее: активно ждать на потоке или спать
- как применять инженерный подход

## Кому может быть интересно, целевая аудитория

- JVM спецы, непосредственно работающие с многопоточным кодом
- JVM спецы, расширяющие свой кругозор, интересующиеся многопоточным программированием

## Мотивация к появлению данного доклада

#### Пункт №1

В 2019 я делал доклад на хайлоде (кому интересно проходите по ссылке), в котором сравнивались решения для 3 классических
задач многопоточности на классической java concurrency и vert.x, потому что классика сложная, а в vert.x прикольный API
и подход.

У меня остался открытый гештальт - честно измерить производительность решений.

#### Пункт №2

Коллеги говорят, установили spring.threads.virtual.enabled=true и все стало быстрее в 2 раза.

#### Пункт №3

Другие коллеги говорят, установили spring.threads.virtual.enabled=true и ничего не изменилось.

#### Пункт №4

Релизнулись виртуальные потоки.

## Ссылка на мой доклад на HighLoad SPb 2019

https://youtu.be/BpjpPrH_0p0

## Минимум знаний, необходимый, но недостаточный для полноценного понимания материала

Ладно, не то чтобы необходимо, но следующие вещи не будут объясняться на пальцах с нуля:

- java concurrency - synchronized, ReentrantLock, CyclicBarrier, AtomicInteger (CAS)
- virtual threads, structured concurrency -
- junit 5
- jmh
- vert.x - (все же немного будет)

## Что разбирается в докладе

Феномены, которые хочется продемонстрировать и мифы, которые хочется развенчать:

- виртуальные потоки создаются легко в большом количестве: 10К, 100К, 1КК
- switch context виртуальных потоков быстрее платформенных
- blocking (on IO) на виртуальных потоках быстрее
- пининг на synchronized реально починили
- решения на vert.x сравнимы по производительности с "классикой"
  (тут и далее под классикой понимаем java concurrency и инструментарий из java.util.concurrent, включая project loom)
- решения на vert.x проще и нагляднее с точки зрения АПИ чем классика
- synchronized по производительности работает хуже ReentrantLock
- инженерный подход (измеряю-анализирую-синтезирую-измеряю) применим для ответа на предыдущие вопросы

## Цитаты для разбора, использования и затравки

#### Loom

...

> Virtual threads are not faster threads — they do not run code any faster than platform threads. They exist to provide
> scale (higher throughput), not speed (lower latency). There can be many more of them than platform threads, so they
> enable the higher concurrency needed for higher throughput according to Little's Law.

...

> To put it another way, virtual threads can significantly improve application throughput when
> - The number of concurrent tasks is high (more than a few thousand), and
> - The workload is not CPU-bound, since having many more threads than processor cores cannot improve throughput in that
    case.

...

> Virtual threads are not cooperative.

...

> Typically, a virtual thread will unmount when it blocks on I/O or some other blocking operation in the JDK, such as
> BlockingQueue.take(). When the blocking operation is ready to complete (e.g., bytes have been received on a socket),
> it
> submits the virtual thread back to the scheduler, which will mount the virtual thread on a carrier to resume
> execution.

...

> The vast majority of blocking operations in the JDK will unmount the virtual thread, freeing its carrier and the
> underlying OS thread to take on new work. However, some blocking operations in the JDK do not unmount the virtual
> thread, and thus block both its carrier and the underlying OS thread. This is because of limitations either at the OS
> level (e.g., many filesystem operations) or at the JDK level (e.g., Object.wait())

...

> There are two scenarios in which a virtual thread cannot be unmounted during blocking operations because it is pinned
> to its carrier:
> - When it executes code inside a synchronized block or method, or
> - When it executes a native method or a foreign function.

...

> The stacks of virtual threads are stored in Java's garbage-collected heap as stack chunk objects.

...

> Unlike platform thread stacks, virtual thread stacks are not GC roots, so the references contained in them are not
> traversed in a stop-the-world pause by garbage collectors, such as G1, that perform concurrent heap scanning. This
> also means that if a virtual thread is blocked on, e.g., BlockingQueue.take(), and no other thread can obtain a
> reference to either the virtual thread or the queue, then the thread can be garbage collected — which is fine, since
> the virtual thread can never be interrupted or unblocked. Of course, the virtual thread will not be garbage collected
> if it is running or if it is blocked and could ever be unblocked.

...

#### Vert'x

...

> A virtual thread verticle is just like a standard verticle but it’s executed using virtual threads, rather than using
> an event loop.

...

> Virtual thread verticles are designed to use an async/await model with Vert.x futures.

...

## План доклада, он же протокол исследований и результатов с выводами

#### Представляюсь, мотивация, прошлый доклад, для кого, озвучиваю план крупными мазками

О себе и все такое

#### Беру прошлое решение для философов на ReentrantLock с хайлода и сильно его упрощаю: убираю случайную задержку на кормлении и смотрю что получается

[_001_reentrant_naive](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_001_reentrant_naive)

Почему разбираем философов: а хорошо масштабируются. Ну и восток наше все!

Начинаю с ReentrantLock, погонял на платформенных потоках и на виртуальных.

На глаз может показаться, что виртуальные потоки показывают худшее время, но я решил верить только бенчмаркам и тестам;

Но, чтобы написать тест или бенчмарк нам не подойдет решение, которое бежит указанное время, надо сделать pivot!

#### Делаю Pivot для ReentrantLock + structured concurrency

[_002_reentrant_pivot](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_002_reentrant_pivot)

Переписываю логику так, чтобы иметь возможность мерить время выполнения с указанным числом философов до того момента
когда один из философов завершит указанное число кормлений.

И для pivot, оказывается, очень подходит structured concurrency: оно само остановит выполнение по достижении одним из
философов указанного числа кормлений.

Вот теперь можно писать тесты и бенчмарки, но прежде ...

#### Добавляю решение для synchronized + Pivot

[_003_synchronized_pivot](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_003_synchronized_pivot)

Беру прошлое решение для философов с synchronized с хайлода и сильно его упрощаю и сразу делаю pivot

#### Теперь пишу тесты на  Pivot-тнутые решения на junit5

[_004_junit5_tests_jdk](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_004_junit5_tests_jdk)

Теперь можно написать юнит тесты.

Нам хорошо подойдет @RepeatedTest(200), уже можно будет сравнивать платформенные потоки с виртуальными и ReentrantLock с
synchronized.

На 200 повторов с 1К философов и 10К кормлений в пике результаты такие:

[Test Results - _004_junit5_tests_jdk_in_jvm-day-2025.html](Test%20Results%20-%20_004_junit5_tests_jdk_in_jvm-day-2025.html)

По тестам уже можно решить, что виртуальные треды на порядок рвут платформенные, при этом под synchronized они быстрее
ReentrantLock. Это мы проверим на бенчмарках! Но позже!

#### Рассказываю некоторые нюансы с компилятором, рантаймом и железом

На разных jdk + jvm получаются немного разные результаты по погрешностям. Разбор почему так происходит выходит за рамки
доклада, средние значения если и различались, то несущественно.

Тесты и бенчмарки делались на:

- openjdk-24.0.1
- liberica-full-24.0.1
- liberica-24.0.1
- temurin-24.0.1

В итоге все компилировалось и бежало на openjdk-24.0.1, конечные результаты также приведены для openjdk-24.0.1.

Все эксперименты, кроме бенчмарка на масштабирование приведены для 1К философов и до 10К кормлений для первого
удачливого философа, после чего бенчмарк или тест считается пройденным и останавливается.

Все тестировалось на моей локельной тачке под управлением macOS 15.5, ТТХ следующие:

- 2,6 GHz 6-Core Intel Core i7
- 16 GB
- диск APPLE SSD AP0512N

На моей тачке для пользовательского java процесса доступно примерно 4060+ платформенных потоков. При переборе летит, как
и ожидалось:

> [0.701s][warning][os,thread] Failed to start thread "Unknown thread" - pthread_create failed (EAGAIN) for attributes:
> stacksize: 1024k, guardsize: 4k, detached.
> [0.702s][warning][os,thread] Failed to start the native thread for java.lang.Thread "Thread-4065"
> Exception in thread "main" java.lang.OutOfMemoryError: unable to create native thread: possibly out of memory or
> process/resource limits reached

Конкретно сколько jvm пользует показывает Thread.activeCount(), но не точно, а примерно. Для виртуальных тредов под
дебаггером вообще бывает виден один активный поток;

И не забываем включить опцию для компилятора и аргумент VM `--enable-preview`, чтоб заработало structured concurrency,
релиз которого планируется в 25 java - ну-ну...

#### Делюсь нюансоми с методологией тестирования и замерами

Изначально я задумывал разделить в тестах и бенчмарках логику инициализации философов и их прогоны, чтобы время
инициализации не считалось и не входило в конечный замер. Однако это сильно усложнило логику сброса состояния философов
между прогонами и в итоге я решил от этого отказаться. Абсолютные числа получились больше, но они ничего не значат сами
по себе, важен только их порядок и отношения.

Эмпирически было установлено, что 3-х разогревочных прогонов почти всегда хватает чтобы боевые замеры ложились кучнее.

Максимальный размер хипа для форкнутого процесса под бенчмарк в 4Гб хватает, чтобы не ловить OOM в кишках vert.x типа
такого на небольших кол-вах филосовов и кормлений:

> Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "
> vertx-blocked-thread-checker"

Однако на больших кол-вах процессы просто замирали. Докапываться до причины я не пошел, есть стойкое ощущение, что это
затык на внутренней шине: кол-во подписок огромно и внутренние события штормят и убивают ресурсы для полезной нагрузки.

vert.x в тестах и бенчмарках по-честному стопается, да, это занимает некоторое время, но убирает ругань в логах vert.x
на жесткое выключение под работающими вертиклами и честно стопает форкнутый процесс бенчмарка, в противном случае
форкнутый процесс висит вечно, jps + kill -9 вам в помощь:

> JMH had finished, but forked VM did not exit, are there stray running threads? Waiting 9 seconds more...

Логирование для vert.x в [vertx-default-jul-logging.properties](src/main/resources/vertx-default-jul-logging.properties)
было отключено, чтобы не тратить время на вывод в консоль.

#### Пишу первые бенчмарки, чтобы уже на что-то опираться - ну здравствуй, jmh!

[_005_jmh_benchmarks_jdk](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_005_jmh_benchmarks_jdk)

Сначала наивно казалось, что если обогатить юнит-тест правильными аннотациями и дописать main метод, то юнит-тест легко
заработает и как бенчмарк, и как юнит-тест.

Но оказалось, что сгенеренный jmh код тоже воспринимается как тесты ибо наследуется от тестов и в итоге запускается как
тест. Это неудобно. Поэтому решил не экономить на копи-пасте.

Используем "черную дыру" для исключения эффектов оптимизаций.

| Benchmark                                                                                      | Mode | Cnt |   Score |  Error | Units |
|:-----------------------------------------------------------------------------------------------|:----:|:---:|--------:|-------:|------:|
| ReentrantLockPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads  | avgt |  7  |   2.088 |  0.122 | ms/op | 
| SynchronizedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads     | avgt |  7  |   2.235 |  0.100 | ms/op |
| SynchronizedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads    | avgt |  7  | 114.038 | 16.327 | ms/op | 
| ReentrantLockPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads | avgt |  7  | 137.708 | 13.519 | ms/op | 

Видно, что виртуальные потоки справляются лучше платформенных на 2 порядка, а synchronized и ReentrantLock сравнимы в
пределах погрешности.

#### Продумываю методологию проверки пининга виртуального потока под synchronized на блокирующем вызове

Выяснили, что с виртуальными потоками мы можем держать огромное кол-во некооперирующих потоков - это хорошо для
промышленных стандартных задач, например обработка запросов на вебсервере.

Но наш текущий
философ [SynchronizedPhilosopher.java](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_003_synchronized_pivot/SynchronizedPhilosopher.java)
не покажет нам пининг, так как внутри "кормления" нет блокирующего метода. Так давайте же сделаем!

Какие варианты на чем можно устроить настоящую блокировку:

- поход по сети по урлу (непредсказуемо из-за самой сети или упремся в производительность сервера)
- вызов блокирующего вызова на блокирующей очереди (непонятно как сделать непротиворечивую логику)
- чтение файла (не идеально, но предсказуемо на SSD и на последовательном чтении)

А сравнивать по производительности будем с:

- активным ожиданием (читай итерация в цикле по времени) на время сопоставимое со временем последовательного чтения из
  файла на SSD
- слипами потока на время сопоставимое со временем последовательного чтения из файла на SSD

Модифицируем всех философов, чтобы они принимали в себя рабочую нагрузку и поочередно замеряем.

Давайте глянем на кусочек из модного систем дезайн интервью:
https://habrastorage.org/r/w1560/getpro/habr/upload_files/20b/769/22f/20b76922f1069403081d4b0818d24970.png

Тут находим опорное значение для чтения, опытным путем приходим к "правильному" размеру файла - 16КB: не слишком долго и
не слишком мало. Ожидаемое время - 16К nanosec

#### Модифицирую философов так, чтобы они принимали в себя рабочую нагрузку

#### Модифицирую бенчмарки и тесты так, чтобы они отдавали в философов рабочую нагрузку и тестирую в 4 вариантах

[_006_jmh_benchmarks_jdk_noop](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_006_jmh_benchmarks_jdk_noop)

| Benchmark                                                                             | Mode | Cnt |   Score |  Error | Units |
|:--------------------------------------------------------------------------------------|:----:|:---:|--------:|-------:|------:|
| NoopPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads  | avgt |  7  |   1.898 |  0.048 | ms/op | 
| NoopPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads    | avgt |  7  |   1.927 |  0.058 | ms/op |
| NoopPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads   | avgt |  7  |  96.351 |  0.709 | ms/op | 
| NoopPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads | avgt |  7  | 129.329 | 15.365 | ms/op | 

Виртуальные потоки на 2 порядка лучше, а synchronized и ReentrantLock сравнимы в пределах погрешности, тут все бьется с
предыдущими измерениями.

Будем аккуратно сравнивать между собой следующие 3 бенчмарки, т. к. можно промахнуться с оценкой операции реального
чтения с SSD.

[_007_jmh_benchmarks_jdk_sleeping](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_007_jmh_benchmarks_jdk_sleeping)

| Benchmark                                                                                     | Mode | Cnt |   Score |   Error | Units |
|:----------------------------------------------------------------------------------------------|:----:|:---:|--------:|--------:|------:|
| SleepingPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_platform_threads | avgt |  7  | 619.487 |  57.005 | ms/op | 
| SleepingPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_platform_threads   | avgt |  7  | 695.648 |  47.757 | ms/op |
| SleepingPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_virtual_threads    | avgt |  7  | 743.421 | 111.426 | ms/op | 
| SleepingPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_virtual_threads  | avgt |  7  | 757.324 |  85.793 | ms/op | 

Тут нет однозначности, я бы сказал, что все сравнимо в пределах погрешностей. Если судить только по средним, то
как-будто платформенные потоки лучше засыпают и просыпаются, но с чего бы это на самом деле?...

[_008_jmh_benchmarks_jdk_blocking_reading](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_008_jmh_benchmarks_jdk_blocking_reading)

| Benchmark                                                                                                    | Mode | Cnt |     Score |    Error | Units |
|:-------------------------------------------------------------------------------------------------------------|:----:|:---:|----------:|---------:|------:|
| BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_platform_threads | avgt |  7  |   585.240 |   94.912 | ms/op | 
| BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_platform_threads   | avgt |  7  |   906.057 |  107.330 | ms/op |
| BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads  | avgt |  7  |  3610.834 |  744.468 | ms/op | 
| BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_virtual_threads    | avgt |  7  | 17666.041 | 5157.029 | ms/op |

Ого. Как я вижу эти результаты?

- виртуальные потоки на порядок или 2 хуже платформенных даже с учетом погрешности
- последнюю строчку я бы интерпретировал либо как пининг виртуального потока на блокирующем апи при вызове внутри
  synchronized, либо как провокацию или багу! Я смотрю на такое же чтение в synchronized на платформенном потоке и не
  понимаю почему виртуальному настолько плохо.
- Да, методология плоха тем, что смешивает измерение производительности при
  борьбе за палочки/мониторы и блокирующее чтение ИЛИ вообще чтение файла было не самой лучшей идеей
- НО самое вероятное и простое объяснение последней строки - пиннинг есть, и пиннится один из немногих (возможно один)
  платформенный поток и чтение становится абсолютным бутылочным горлышком
- лучшие результаты по блокирующему чтению под мониторами по времени похожи на слип, но хуже в 2,5 раза активного
  ожидания - активно ждать дешевле слипа!

[_009_jmh_benchmarks_jdk_active_waiting](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_009_jmh_benchmarks_jdk_active_waiting)

| Benchmark                                                                                                | Mode | Cnt |   Score |  Error | Units |
|:---------------------------------------------------------------------------------------------------------|:----:|:---:|--------:|-------:|------:|
| ActiveWaitingPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_virtual_threads    | avgt |  7  | 170.812 |  7.355 | ms/op | 
| ActiveWaitingPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_virtual_threads  | avgt |  7  | 178.575 | 16.057 | ms/op |
| ActiveWaitingPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_platform_threads   | avgt |  7  | 303.367 | 12.389 | ms/op | 
| ActiveWaitingPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_platform_threads | avgt |  7  | 416.446 | 92.891 | ms/op |

Активное ожидание показывает лучшие результаты на виртуальных потоках в 1,5 раза, остальное в пределах погрешностей.

Косвенно это может означать, что если вам не повезло с драйвером или клиентом и он не уходит в IO, то на виртуальных
потоках под нагрузкой все равно может получиться профит за счет более быстрого переключения контекста, НО ТОЛЬКО если
нет затыков на самом драйвере или клиенте.

Другой осторожный вывод снова подтверждается: активно ждать эффективнее слипа.

#### В этом месте по идее надо уходить и строить специальный тест без захвата 2-х палок чисто на блокирующее чтение из-под synchronized...

#### Вместо этого я слегка ухожу в сторону и возвращаемся к акторной модели и vert.x

#### Стремительно провожу минимальный ликбез по vert.x + акторной модели

- Мультиреактор
- На моем компе 12 логических ядер - в vert.xе будет мультиреактор на 12 потоков
- Виды вертиклов, новый тип
- Особенность кода для виртуальных вертиклов
- Ликбез Очереди+топики+sharedata - то что надо понять для примеров

#### Реализую философов на vert.x сразу так, чтобы затаскивать в тесты и в бенчмарки (pivot)

[_010_vertx_pivot](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_010_vertx_pivot)

Код получился чище - без классической конкарренси совсем!

[_011_junit5_tests_vertx](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_011_junit5_tests_vertx)

Затащил тестовую библиотеку для vert.xа. Удобно.

[Test Results - _011_junit5_tests_vertx_in_jvm-day-2025.html](Test%20Results%20-%20_011_junit5_tests_vertx_in_jvm-day-2025.html)

По тестам виртуальные вертиклы рвут те что в event-loop в 1,5 раза, посмотрим как в бенчмарке!

[_012_jmh_benchmarks_vertx_noop](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_012_jmh_benchmarks_vertx_noop)
[_013_jmh_benchmarks_vertx_blocking_reading](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_013_jmh_benchmarks_vertx_blocking_reading)
[_014_jmh_benchmarks_vertx_active_waiting](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_014_jmh_benchmarks_vertx_active_waiting)
[_015_jmh_benchmarks_vertx_sleeping](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_015_jmh_benchmarks_vertx_sleeping)

Тут делаю следующие допущения:

- event-loop блокировать нельзя, блокирующее чтение и слипы в vert.x запрещены - это надо делать либо в рабочих
  вертиклах либо в виртуальных, т. е. существующие стратегии constructXYZEating() по-хорошему не подойдут для event-loop
  вертиклов - НО МЫ НАРУШИМ!!
- в vert.x есть асинхронное/неблокирующее чтение файла - надо решить насколько честно было использовать его - НО МЫ НЕ
  БУДЕМ ЕГО ИСПОЛЬЗОВАТЬ!!
- можно через таймер красиво решить активное ожидание - НО МЫ НЕ БУДЕМ ЭТОГО ДЕЛАТЬ!!

#### Для удобства объединеняю бенчмарки всех типов

[_099_jmh_benchmarks_united](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_099_jmh_benchmarks_united)

Для удобства проведения всех измерений за один прогон объединяю все бенчмарки в один

| Benchmark                                                                                           | Mode | Cnt |     Score |    Error | Units |
|:----------------------------------------------------------------------------------------------------|:----:|:---:|----------:|---------:|------:|
| UnitedPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads              | avgt |  7  |     2.093 |    0.220 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads                | avgt |  7  |     2.397 |    0.164 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads               | avgt |  7  |   109.763 |   16.223 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads             | avgt |  7  |   131.368 |   16.621 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_virtual_threads      | avgt |  7  |   171.617 |    4.052 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_virtual_threads    | avgt |  7  |   179.902 |   29.410 | ms/op | 
| UnitedPhilosophersBenchmark.test_virtual_noop_verticle_philosophers                                 | avgt |  7  |   206.376 |   12.699 | ms/op | 
| UnitedPhilosophersBenchmark.test_verticle_noop_philosophers                                         | avgt |  7  |   288.937 |   26.813 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_platform_threads     | avgt |  7  |   325.382 |   34.030 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_platform_threads   | avgt |  7  |   439.129 |  111.866 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_platform_threads | avgt |  7  |   548.097 |  122.230 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_platform_threads         | avgt |  7  |   611.358 |  102.814 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_virtual_threads          | avgt |  7  |   649.195 |   68.691 | ms/op | 
| UnitedPhilosophersBenchmark.test_active_waiting_verticle_philosophers                               | avgt |  7  |   678.343 |   22.580 | ms/op | 
| UnitedPhilosophersBenchmark.test_virtual_active_waiting_verticle_philosophers                       | avgt |  7  |   718.618 |   59.118 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_platform_threads           | avgt |  7  |   767.795 |   89.405 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_virtual_threads            | avgt |  7  |   845.711 |   78.225 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_platform_threads   | avgt |  7  |   929.553 |   73.032 | ms/op | 
| UnitedPhilosophersBenchmark.test_blocking_reading_verticle_philosophers                             | avgt |  7  |  2820.554 |  147.504 | ms/op | 
| UnitedPhilosophersBenchmark.test_virtual_blocking_reading_verticle_philosophers                     | avgt |  7  |  3229.174 |  106.756 | ms/op | 
| UnitedPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads  | avgt |  7  |  3578.460 |  558.093 | ms/op | 
| UnitedPhilosophersBenchmark.test_sleeping_verticle_philosophers                                     | avgt |  7  |  3596.082 |  412.365 | ms/op | 
| UnitedPhilosophersBenchmark.test_virtual_sleeping_verticle_philosophers                             | avgt |  7  |  3953.206 |  232.746 | ms/op | 
| UnitedPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_virtual_threads    | avgt |  7  | 16583.318 | 4071.447 | ms/op | 

Комментарии

- быстрее всего в "классике" либо ничего не делать, либо активно ждать, и где-то рядом болтается ничего не делать в
  vert.x.
- подтверждается на порядок просадка в производительности в блокирующем вызове виртуального потока под synchronized, что
  особенно странно, т. к. относительно такого же для платформенного варианта имеем на порядок лучше показатель хотя там
  УЖЕ и есть натурально запиненый платформенный поток! Но самое простое и вероятное объяснение все же такое: пиннинг
  остался, и пиннится один из немногих (возможно единственный!) платформенный поток и чтение становится абсолютным
  бутылочным горлышком; в случаем с решением на платформенных потоках их тупо больше!

#### А теперь попробую помасштабировать наши бенчмарки в 2-х измерениях: по кол-ву философов и по кол-ву "кормлений"

[_999_jmh_benchmarks_scaled](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_999_jmh_benchmarks_scaled)

В масштабируемый сводный бенчмарк не вошли решения на vert.x с большими числами философов и кормлений.
Это потому что такие бенчмарки залипают! Не вдаваясь в подробности, кажется, что шторм событий от подписок и отписок
просто выносит всю полезную нагрузку. Ну а что с десятками тысяч вертиклов которые хотя бы дорабатывают до конца?

Также в масштабируемый сводный бенчмарк не вошли решения на платформенных потоках - там не помасштабируешь по кол-ву
филосов, и не очень интересное масшабирование по кол-ву кормлений (увидим ниже)

| Benchmark                                                                                               | Mode | Cnt |    Score |    Error | Units |
|:--------------------------------------------------------------------------------------------------------|:----:|:---:|---------:|---------:|------:|
| ScaledPhilosophersBenchmark._020_test_0010K_0010K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |   13.453 |    2.037 | ms/op | 
| ScaledPhilosophersBenchmark._010_test_0010K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |   13.790 |    0.852 | ms/op | 
| UnitedPhilosophersBenchmark.__________0001K_0010K_virtual_noop_verticle_philosophers                    | avgt |  5  |  204.009 |   20.128 | ms/op | 
| UnitedPhilosophersBenchmark.__________0001K_0010K_verticle_noop_philosophers                            | avgt |  5  |  287.172 |   31.099 | ms/op | 
| ScaledPhilosophersBenchmark._040_test_0010K_0010K_noop_verticle_philosophers                            | avgt |  7  |  527.384 |  330.028 | ms/op | 
| ScaledPhilosophersBenchmark._030_test_0010K_0010K_virtual_noop_verticle_philosophers                    | avgt |  7  | 1028.263 | 1976.565 | ms/op |

vert.x хуже на порядок классики! Погрешности такие, что вообще непонятно, что намеряли и как прогнозировать нагрузку?!
Оснований доверять таким числам нет, поэтому далее оставляем только измерения для "классики".

| Benchmark                                                                                               | Mode | Cnt |    Score |   Error | Units |
|:--------------------------------------------------------------------------------------------------------|:----:|:---:|---------:|--------:|------:|
| UnitedPhilosophersBenchmark._____test_0001K_0010K_reentrant_lock_philosophers_with_virtual_thread       | avgt |  5  |    1.952 |   0.188 | ms/op | 
| UnitedPhilosophersBenchmark._____test_0001K_0010K_test_synchronized_philosophers_with_virtual_threads   | avgt |  5  |    2.184 |   0.439 | ms/op | 
| ScaledPhilosophersBenchmark._020_test_0010K_0010K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |   13.453 |   2.037 | ms/op | 
| ScaledPhilosophersBenchmark._010_test_0010K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |   13.790 |   0.852 | ms/op |
| ScaledPhilosophersBenchmark._100_test_0010K_0100K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |   20.490 |   0.973 | ms/op |
| ScaledPhilosophersBenchmark._090_test_0010K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |   21.520 |   2.153 | ms/op |
| ScaledPhilosophersBenchmark._220_test_0010K_1000K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |   70.700 |   1.221 | ms/op |
| ScaledPhilosophersBenchmark._210_test_0010K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |   72.035 |   1.424 | ms/op |
| ScaledPhilosophersBenchmark._060_test_0100K_0010K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |  276.019 |  92.038 | ms/op |
| ScaledPhilosophersBenchmark._050_test_0100K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |  284.643 |  35.604 | ms/op |
| ScaledPhilosophersBenchmark._130_test_0100K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |  297.571 |  37.570 | ms/op |
| ScaledPhilosophersBenchmark._140_test_0100K_0100K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |  255.395 | 102.263 | ms/op |
| ScaledPhilosophersBenchmark._142_test_0100K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  |  351.140 | 102.412 | ms/op |
| ScaledPhilosophersBenchmark._144_test_0100K_1000K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  |  382.512 |  60.712 | ms/op |
| ScaledPhilosophersBenchmark._180_test_1000K_0010K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  | 3847.501 | 197.755 | ms/op |
| ScaledPhilosophersBenchmark._170_test_1000K_0010K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  | 4307.880 | 402.627 | ms/op |
| ScaledPhilosophersBenchmark._182_test_1000K_0100K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  | 3993.247 | 530.418 | ms/op |
| ScaledPhilosophersBenchmark._184_test_1000K_0100K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  | 4089.308 | 576.735 | ms/op |
| ScaledPhilosophersBenchmark._250_test_1000K_1000K_reentrant_lock_noop_philosophers_with_virtual_threads | avgt |  7  | 3931.608 | 460.112 | ms/op |
| ScaledPhilosophersBenchmark._260_test_1000K_1000K_synchronized_noop_philosophers_with_virtual_threads   | avgt |  7  | 3792.618 | 352.711 | ms/op |

Что я тут вижу:

- в "классике" очень логичная "лесенка" по порядкам философов: увеличиваем кол-во философов на порядок - время
  увеличивается на порядок, т е предсказуемое, линейное масштабирование, прям мечта любого инженера.
- в классике целевое кол-во кормлений совсем незначительно влияет на время выполнения - не на порядки, как с ростом
  философов: небольшой, линейной рост и даже иногда в пределах погрешности.
- synchronized vs reentrant_lock сравнимы с точностью до погрешности

Так что же получается, "классика" прям молодец и инженеры, которые контрибьютят в жаву не зря едят свой хлеб с Маслоу??!
Да, возможно, кроме потенциально недофикшеного бага с пинингом , который может оказаться драмматическим!

## Итоги и выводы

- измерения получается инструментально выполнить и на "классике", и на vert.x. На классике проще. На vert.x уже есть
  утильные классы для юнитов, но для бенчмарка vert.x приходится призывать "классику" "к барьеру".
- классика масштабируется прогнозируемо, линейно, как мы любим
- классика сложнее - требует более высокого уровня квалификации jav программиста
- vert.x и акторная модель вообще проще как API и как концепт, пакета конкарренси нет от слова совсем, берем 10К джунов
  и клепаем энтерпрайз
- structured concurrency уже работает в превью 24й и оно удобное, например, стало проще закрывать весь пул задач
- synchronized против ReentrantLock - в основном различий не видно, все в пределах погрешности
- пининг - смотря под каким углом смотреть: возможно да, возможно методология нехорошая (надо бы выделенный тест) но по
  замерам не пофиксили
- блокирующий код vs активное ожидание vs слип - лучше активно ждать чем спать (как по-философски), оценка по времени
  блокирующего чтения может быть неточной, не оцениваем и не сравниваем.
- что не так с vert.x на масштбировании? vert.x не готов к такому жесткому использованию локально, внутренняя
  механика подписок и отписок и шторм на event-bus мешает, а не помогает (это только добротная гипотеза, по-хорошему
  надо проверять и может кому-то удастся правильно "приготовить" vert.x)
- стал бы я прагматично голосовать за vert.x в данном контексте в 2019 - да! (даешь новый проект на новой технологии, не
  спринг - ура-ура, команда воодушевлена, риски все мои, комьюнити маленькое, помощи ждать неоткуда, спека плохая - поле
  чудес для профессионального роста), потому что задача утилизации CPU решалась 100% (равно как и прочей
  реактивщиной/асинхронщиной)
- стал бы я прагматично голосовать за vert.x в данном контексте в 2025 - нет, конечно, зачем? Есть же виртуальные
  потоки!

## Чекаю по пунктам удалось ли покрыть заявленные феномены и мифы:

- виртуальные потоки создаются легко в большом количестве: 10К, 100К, 1КК - ДА, создаются и еще как
- switch context виртуальных потоков быстрее платформенных - ДА, быстрее однозначно
- blocking (on IO) на виртуальных потоках быстрее - НЕТ, мои замеры показывают строго обратное, возможно косяк
  методологии
- пининг на synchronized в виртуальном потоке реально починили - НЕТ, по моим замерам не починили
- решения на vert.x сравнимы по производительности с "классикой" - НЕТ, так утверждать нельзя никак
- решения на vert.x проще и нагляднее с точки зрения АПИ чем классика - ДА, это точно так
- synchronized по производительности работает хуже ReentrantLock - НЕТ, они сравнимы друг с другом в пределах
  погрешности
- инженерный подход (измеряю->анализирую->синтезирую->измеряю) применим для ответа на предыдущие вопросы - ДА, вполне,
  этот доклад как раз про это

## Полезные ссылки

https://wiki.openjdk.org/display/loom/Main

https://openjdk.org/jeps/444

https://openjdk.org/jeps/505

https://en.wikipedia.org/wiki/Dining_philosophers_problem

https://spring.io/blog/2022/10/11/embracing-virtual-threads

https://vertx.io/docs/5.0.0/vertx-core/java/#virtual_threads

https://vertx.io/docs/5.0.0/vertx-junit5/java/
