# TODO тут будет название доклада

## Описание
TODO тут будет описание доклада

## Кому может быть интересно
TODO

## Мотивация к появлению данного доклада
TODO
 
## TODO Ссылка на доклад на хайлоде

## TODO Минимум знаний, необходимый для понимания:
 - concurrency - synchronized, ReentrantLock, CyclicBarrier, AtomicInteger (CAS)
 - junit 5
 - jmh
 - Virtual threads, Structured Concurrency - необязательно, но не будут объясняться на пальцах с нуля
 - https://vertx.io (если войдет в доклад) - только то что надо для понимания доклада
 
## TODO Что разбирается в проекте

Тут надо определить какие именно феномены хочется продемонстрировать и мифы развенчать:

Претенденты
 - очевидный - кол-во потоков 10К, 1М
 - switch context (быстрее) 
 - blocking (on IO)
 - есть пининг или реально починили

Формат разбора задач: 
 1. постановка задачи
 1. решение synchronized
 1. решение ReentrantLock
 1. (под вопросом) решение на vert.x без блокировок
 1. сравнение производительности, результаты JMH  
 1. масштабирование раз  
 1. масштабирование два  
 1. сравнительный анализ
 1. добавляем виртуальности во все и все заново
 1. общее сравнение

## TODO Итоги выводы

## Ссылки
https://wiki.openjdk.org/display/loom/Main
https://openjdk.org/jeps/444
https://openjdk.org/jeps/505
https://en.wikipedia.org/wiki/Dining_philosophers_problem
https://spring.io/blog/2022/10/11/embracing-virtual-threads
https://vertx.io
https://vertx.io/docs/4.5.14/vertx-core/java/#virtual_threads
https://vertx.io/docs/4.5.14/vertx-junit5/java/

## Цитаты для разбора и использования за и против и затравки

Virtual threads are not faster threads — they do not run code any faster than platform threads. They exist to provide scale (higher throughput), not speed (lower latency). There can be many more of them than platform threads, so they enable the higher concurrency needed for higher throughput according to Little's Law.

To put it another way, virtual threads can significantly improve application throughput when
 - The number of concurrent tasks is high (more than a few thousand), and
 - The workload is not CPU-bound, since having many more threads than processor cores cannot improve throughput in that case.

Virtual threads are not cooperative.

Typically, a virtual thread will unmount when it blocks on I/O or some other blocking operation in the JDK, such as BlockingQueue.take(). When the blocking operation is ready to complete (e.g., bytes have been received on a socket), it submits the virtual thread back to the scheduler, which will mount the virtual thread on a carrier to resume execution.

The vast majority of blocking operations in the JDK will unmount the virtual thread, freeing its carrier and the underlying OS thread to take on new work. However, some blocking operations in the JDK do not unmount the virtual thread, and thus block both its carrier and the underlying OS thread. This is because of limitations either at the OS level (e.g., many filesystem operations) or at the JDK level (e.g., Object.wait())

There are two scenarios in which a virtual thread cannot be unmounted during blocking operations because it is pinned to its carrier:
 - When it executes code inside a synchronized block or method, or
 - When it executes a native method or a foreign function.

The stacks of virtual threads are stored in Java's garbage-collected heap as stack chunk objects.

Unlike platform thread stacks, virtual thread stacks are not GC roots, so the references contained in them are not traversed in a stop-the-world pause by garbage collectors, such as G1, that perform concurrent heap scanning. This also means that if a virtual thread is blocked on, e.g., BlockingQueue.take(), and no other thread can obtain a reference to either the virtual thread or the queue, then the thread can be garbage collected — which is fine, since the virtual thread can never be interrupted or unblocked. Of course, the virtual thread will not be garbage collected if it is running or if it is blocked and could ever be unblocked.

## Повествование в формате мозгового штурма и истории героя

Навеяно spring.threads.virtual.enabled=true и все стало быстрее или не стало

Я делал доклад. Смотрите как там все было (ссыдка на хайлод)

Еще остался открытый гештально про точные измерения, тесты конечно показывали сравнительно схожие результаты, но без официальных
jmh замеров я не чувствовал что могу что-то утверждать, формулировки были обтекаемые

И в подумал а что будет если просто взять и на философах все померять. Дополнитеьно зарелилился проект Лум поэтому стало 
актуально провести измерения и для  виртуальных потоков

ТУТ МОЖНО провести голосования про ожидания по скорости для философов для 1К штук, для ReentrantLock synchronized и вертекса для 
платформенных и виртуальных потоков - зафиксировать и может даже сразу заспойлерить как оно вышло. 

Взял старый код. упростил его до предела и запустил и .. неповерил

Какая то проблема с остановкой потоков через exit или Timer?
Неужели атомик внутри виртуальных потоков что делает что становится хуже платформенных потоков?
Виртуальные хуже, по производительности, но платформенных 10К не создать

Кстати, а как узнать сколько платформенных потоков можно создать неэмпирически?

Нет, кажется мы реально просто не успеваем создать все потоки - проверим - вернул барьер
Может проблема в jdk? Меняем с либерики на оракл, с платформенными потоками тот же рез (24,0,1) а с виртуальными в 2 раза лучше!!!
Вот это да, может проблема в полной версии - качаю не полную версию либерики - результат лучше чем у полной, хуже чем у оракловой
Ну и контролный выстрел в голову - проверить чо как на ea openjdk 25й - не завелось в IDE : it is configured to use JDK 0, but IDE supports compilation using JDK 7 and newer only
Итого - ничего не понятно, пару раз на фул тоже показало норм результат, еще залипает барьер, это совсем нехорошо - РАЗОБРАТЬСЯ БЫ ПОЧСЕПУ, все равно всплывет на тестах jmh
Переключаюсь на опен ждк
Какие тут возможны выводы - взятие и отпускание блокировок происходит эффективнее, т к пдатформенный поток не уходит в блок
Но важно отметить, что виртуальные потоки НЕ для этого были сделаны, а как раз наоброт для некооперативного взаимодействия

Оценку доступных платформенных потоков через Thread.activeCount()

Для 5 тыс фил всего 3 платформенных потока! Только один из них сам таймер, второй шатдаун хук жвм, мейн уже ушел, под дебагером вообще куча, сособенно в пулах форк джойна - короче, так себе эстимейт

Для платформенных потоков - 1003 - ну тыс понятно, а 3 оставшихся непонятно

На моей тачке для пользовательского жава процесса 4060+ примерно платформенных потоков : 2,6 GHz 6-Core Intel Core i7  16 GB

тут надо сделать пивот для кода и считать время для 1 тыс до 1млн попыток с jmh, протестировать метод и подтвердить ускорение
Пивот также поможет нам избавиться от CAS, упростим логику - дождемся пока 1 из философов поест определенное кол-во раз (это будет последний)
И остановимся- вот теперь можно считать время выполнения и пилить бенчмарк на JMH
Также кажется что нам может помочь Structured Concurrency - заодно проверим как оно работает и что требует от кода.

Не забываем включить --enable-preview в аргументы компилятора и рантайма для 24й жавы, релиз планируется в 25 - ну-ну

Так как по умолчанию под капотом виртуальные потоки то явно передаем фабрику
С платформенными потоками все как ожидается:
[0.701s][warning][os,thread] Failed to start thread "Unknown thread" - pthread_create failed (EAGAIN) for attributes: stacksize: 1024k, guardsize: 4k, detached.
[0.702s][warning][os,thread] Failed to start the native thread for java.lang.Thread "Thread-4065"
Exception in thread "main" java.lang.OutOfMemoryError: unable to create native thread: possibly out of memory or process/resource limits reached

ТОлько что-то очень быстро... Не верю меняем булеан на инт
И таки да - 200мс на платформенных тредах
А что на виртуальных - где-то также, давайте поднимем до 10М кормлений 
10М кормлений - вот теперь больше секунды на виртуальных, и меньше секунды на платформенных - можно и подрубать jmh и разбираться как так!

Но до этого давайте проверим а есть ли разница с синхронайзами вместо ReentrantLock
На первый взгляд платформенные типа секунды а виртульные пошустрее, опять же жмх покажет

Кстати а может можно не только jmh попробовать для оценки? Может ли юнит тестом?
Это может быть удобно для отделения логики инициализации от логики "кормления". Также идея показывает время исполнения а еще можно заюзать параметризованные тесты для множественных прогонов с разным колвом философов
С разным получилось неудовно отделять инит от кормления, погонял с 1К заюзал @RepeatableTest
Разница на 100 прогонов: 
1мин 15 сек против 1 мин 2 сек
1мин 16 сек против 1 мин 9 сек

Очень удобно наш тест превратить в бенчмарк - просто добавляем аннотаций и main
Правда сгенеренные jmh классы гаследуются от тестов и как тесты тоже воспринимаются, надо их исключить по регэкспу .*jmhType.* и убрать в ide
Но в итоге для запуска не очень удобно, копируем в родной src

openjdk-24.0.1

Benchmark                                                                                  Mode  Cnt    Score    Error  Units
SynchronizedPhilosophersBenchmark.    test_synchronized_philosophers_with_virtual_threads  avgt   10  554.518 ± 33.901   ms/op
ReentrantLockPhilosophersBenchmark. test_reentrant_lock_philosophers_with_virtual_threads  avgt   10  628.616 ± 46.746   ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  815.053 ± 123.329  ms/op
SynchronizedPhilosophersBenchmark.   test_synchronized_philosophers_with_platform_threads  avgt   10  878.041 ± 13.191   ms/op


Для нескольких jdk, для нескольких количеств, окружение будет локальным, но максимально одинаковым - перебор, надо быть прагматичнм 
Но тем не менее а что там с либерикой фул? не перекомпилируем, байткод от openjdk-24.0.1

liberica-full-24.0.1

Benchmark                                                                                  Mode  Cnt    Score     Error  Units
SynchronizedPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads      avgt   10  609.942 ± 69.725  ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10  794.222 ±  30.801  ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  860.768 ± 244.107  ms/op
SynchronizedPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads     avgt   10  973.529 ± 56.013  ms/op

Ага, т е принципиально все как в openjdk, только немного медленнее
ЛАдно, обычную либерику, тимурин и все!

liberica-24.0.1

Benchmark                                                                                  Mode  Cnt    Score     Error  Units
SynchronizedPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads      avgt   10  621.846 ± 17.671  ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10  656.492 ± 46.289  ms/op
SynchronizedPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads     avgt   10  845.692 ± 74.872  ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  850.776 ± 313.852  ms/op

temurin-24.0.1

Benchmark                                                                                  Mode  Cnt    Score    Error  Units
SynchronizedPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads      avgt   10  587.261 ± 40.373  ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10  682.728 ± 68.705  ms/op
ReentrantLockPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  857.365 ± 300.106  ms/op
SynchronizedPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads     avgt   10  885.470 ± 78.029  ms/op


Интересно, а есть ли разница с синхронайзом? 
Пишем точно такой же код, тестируем с jmh

Ладно, выяснили что с виртуальными потоками мы можем держать огромное кол-во некооперирующих потоков - хорошо для промышленных стандартных задач! а именно обработка запросов на вебсервере

Но это не покажет нам пининг, так как внутри нет блокирующего метода, так давайте же сделаем - урл(-)? очередь(?)?

Модифицируем философов чтобы они принимали в себя рабочую нагрузку через Runnable и далее выбираем что-нить с блокировкой и проверяем
что случится со временем исполнения - TODO next

Для удобства объединяем бенчмарки с философами поддерживающими нагрузку

Benchmark                                                                           Mode  Cnt     Score     Error  Units
UnitedPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10   646.277 ±  42.920  ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10   753.216 ±  41.038  ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10   866.056 ± 137.308  ms/op
UnitedPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10  1041.406 ±  68.076  ms/op

Теперь в теории - что будет если мы уснем внутри кормления: ожидаю что время до 1М сильно увеличится,стоит сделать 3 теста 
 - просто слип
 - трушный блокирующий вызов
 - вызов не отпускающий процессор
И сравнить

Как выбрать слип, что будем тетсировать? Будем целиться в трушный блокирующий вызов, чтобы испытать всю мощь виртуальных потоков

Давайте глянем на кусочек из модного систем дезайн интервью

https://habrastorage.org/r/w1560/getpro/habr/upload_files/20b/769/22f/20b76922f1069403081d4b0818d24970.png

По сети не хочется ходить, можно попробовать читать из файловой системы последовательно - тоже считай поток, но не 1МВ а 16Кб - 1мс * 0,016
read sequentially from SSD 16КБ
С 10 млн кормлений вообще не пошло, надо уменьшать. 10К кормлений вроде норм, время получается примерно как в предыдущих измерениях:

Со слипами выглядит так:

2K nano

Benchmark                                                                             Mode  Cnt    Score     Error  Units
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10  300.200 ±  26.227  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10  489.348 ±  44.136  ms/op
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  558.963 ± 115.968  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10  911.034 ± 178.677  ms/op

4K nano

Benchmark                                                                             Mode  Cnt    Score     Error  Units
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10  306.614 ±  28.454  ms/op
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  583.215 ±  38.467  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10  600.634 ±  56.313  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10  856.902 ± 154.720  ms/op

как-то не очень четко, попробую увеличить

16K nano

Benchmark                                                                             Mode  Cnt     Score     Error  Units
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10   735.126 ±  62.426  ms/op
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10   779.768 ±  61.017  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10   986.950 ± 491.154  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10  1039.079 ± 115.318  ms/op

Получается что виртульные треды и правда спят эффективнее платформенных, а синхронайзд хуже в 1,5 раза и на платформенных и на виртуальных - почему?
Пининг или не пининг?

Ладно, теперь блокируещее чтение 16К файла

Benchmark                                                                            Mode  Cnt      Score      Error  Units
ReadingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10    700.584 ±   58.811  ms/op
ReadingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10    977.999 ±   57.780  ms/op
ReadingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10   2096.228 ±  351.681  ms/op
ReadingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10  16220.032 ± 3672.652  ms/op

Очень неожиданный результат, есть подозрение что бенчмарка врет, давайте попробуем еще раз сделать что0нить чтобы было честнее - Blackhole!
Ну или чтение с фаайловой системы не самый лучший выбор!

Но для начала просто повторим 

Benchmark                                                                            Mode  Cnt      Score      Error  Units
ReadingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10    757.588 ±  186.689  ms/op
ReadingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10    992.817 ±  135.487  ms/op
ReadingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10   2480.131 ±  348.322  ms/op
ReadingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10  13798.372 ± 1787.083  ms/op

Повторили - виртуальные потоки вообще не очень - почему? Сдается мне бенчмарки могут врать и возможно обманывает оптимизация - следующим этапом надо переработать 
под BlackHole и перепроверить - TODO next

А теперь предположим что нам попался неудачный клиент или драйвер, который ожидает на активном потоке

Benchmark                                                                            Mode  Cnt    Score    Error  Units
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt   10  183.055 ±  7.296  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt   10  197.812 ±  4.353  ms/op
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt   10  420.395 ± 89.713  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt   10  510.710 ± 70.167  ms/op

Активное ожидание слишком быстрое - либо неправильно оценили время чтения либо оптимизации - точно пора внедрять черную дыру

Внедряю черную дыру, решаю что тесты буду гонять с самого начала для 10К максимальных кормлений чтобы одинаково для всех

Юнит Тесты на 100 повторов на 1К фил:

12.45 s ReentrantLockPhilosophersTest
11.90 s test_reentrant_lock_philosophers_with_platform_threads()
555 ms test_reentrant_lock_philosophers_with_virtual_threads()
11.51 s SynchronizedPhilosophersTest
11.13 s test_synchronized_philosophers_with_platform_threads()
385 ms test_synchronized_philosophers_with_virtual_threads()

Виртуальные потоки на несколько порядков лучше за счет переключ контекста

UnitedPhilosophersBenchmark

Benchmark                                                                           Mode  Cnt    Score    Error  Units
UnitedPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5    3.046 ±  0.455  ms/op
UnitedPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5    3.161 ±  0.333  ms/op
UnitedPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5  154.629 ± 20.801  ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5  189.116 ± 95.836  ms/op

Виртуальные потоки на 2 порядка лучше за счет переключ контекста

SleepingPhilosophersBenchmark
Benchmark                                                                             Mode  Cnt    Score     Error  Units
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5  575.961 ± 126.248  ms/op
SleepingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5  729.972 ± 123.697  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5  729.597 ± 109.290  ms/op
SleepingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5  793.082 ± 174.496  ms/op

Со слипами все странно, надо повторить

ReadingPhilosophersBenchmark

Benchmark                                                                            Mode  Cnt     Score     Error  Units
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5  175.759 ±  13.147  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5  177.814 ±  24.459  ms/op
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5  397.453 ±  68.143  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5  458.060 ± 240.942  ms/op

Чтение из файла в 2 раза лучше на блокирующих операциях - это прямо то что говорят коллеги!

LoopingPhilosophersBenchmark

Benchmark                                                                            Mode  Cnt    Score     Error  Units
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5  187.176 ±  16.312  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5  192.061 ±  10.334  ms/op
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5  426.538 ± 178.700  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5  521.847 ± 138.848  ms/op

Тут похоже на ожидания: виртуальные треды ведут себя лучше за счет более легкого переключения контекста хотя не то чтобы сильно 
отличалось от трушной блокировки, Может не угадали с оценкой длительности операции, не видно что cpu bound операция прибивает виртуальный поток
Соотношения по временам между вирт и платф потоками одинаковые, т е что трушный блок что активной ожидание на процессоре не показывает сильного отличия

Как сделать так чтобы было видно? Давайте сильно увеличим размер файла и поднимем время активного ожидания? До 64КБ

Benchmark                                                                            Mode  Cnt      Score       Error  Units
ReadingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5   1152.440 ±   331.182  ms/op
ReadingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5   1408.999 ±   530.439  ms/op
ReadingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5  12158.605 ±  2612.381  ms/op
ReadingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5  42778.921 ± 13806.293  ms/op

Что за нафиг, почему трушный блок так плох на виртуальных тредах??? Почему синхронайзд так плох на виртуальных потоках - пининг не починили а еще и сломали?
Прям уверен что фигня, когда время улетает за 10 сек это нон сенс

LoopingPhilosophersBenchmark - на 64КБ

Benchmark                                                                            Mode  Cnt     Score     Error  Units
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5   871.130 ± 201.806  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5   912.384 ± 429.636  ms/op
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5  1186.837 ± 607.236  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5  1807.083 ± 649.841  ms/op

Тут виртуальные потоки лучше, может побольше сделать размер? 256КБ


Benchmark                                                                            Mode  Cnt     Score     Error  Units
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_virtual_threads     avgt    5  3142.873 ± 211.048  ms/op
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_virtual_threads   avgt    5  3176.046 ± 474.272  ms/op
LoopingPhilosophersBenchmark.test_synchronized_philosophers_with_platform_threads    avgt    5  4788.984 ± 678.514  ms/op
LoopingPhilosophersBenchmark.test_reentrant_lock_philosophers_with_platform_threads  avgt    5  5083.694 ± 972.692  ms/op

Я вот не знаю как эти результаты интепретировать - разброс сильный, виртуальные потоки выглядят лучше, но..  

Может нафиг этих филосовов, давайте отделим мух от котлет?
Еще раз что мы хотим
 - проверить пининг на синкронайзд - пофикшен или нет, ожидаю что да
 - проверить трушную блокировку - ожидаю что виртуальные потоки тут как раз и будут "раза в 2 быстрее"
 - проверить "плохой" драйвер, с активным ожиданием на потоке - ожидаю что виртуальные потоки тут НЕ будут "раза в 2 быстрее"

Нужен конкретный бенчмарк на это!

А теперь как насчет слегка уйти в сторону и вернуться к акторной модели и чисто по приколу запилить философов на вертексе?

(тут надо дать мин ликбез по вертексу+акторной модели)

Код получился чище - без конкарренси совсем! Скорость будем мерять в тестах и бенчмарках, фаза инициализации пока слеплена с основным циклом, 
потом подумаю как разделить создание вертиклов чтобы было отдельно и их переисползование, пока все в одном флаконе 
Затащил тестовуб библиотеку для вертекса, получилось отделить инициализацию от кормления

С кодом есть какие то приколы, пока не могу объяснить как может получиться такое:

finish eating at 2025-05-15T16:08:50.767155Z, msg: VerticlePhilosopher #859 has reached 10003 attempts to eat!
finish eating at 2025-05-15T16:08:50.767202Z, msg: VerticlePhilosopher #859 has reached 10004 attempts to eat!
finish eating at 2025-05-15T16:08:50.767249Z, msg: VerticlePhilosopher #859 has reached 10005 attempts to eat!
finish eating at 2025-05-15T16:08:50.767310Z, msg: VerticlePhilosopher #859 has reached 10006 attempts to eat!
finish eating at 2025-05-15T16:08:50.767393Z, msg: VerticlePhilosopher #859 has reached 10007 attempts to eat!

Ведь на 10К точно должны перестать слать себе лупы
Время получается какое-то бешеное

Но мы понимаем что у нас на моем компе 12 или 24 потоков в мультиреакторе, давайте пробовать
ВИРТУАЛЬНЫЕ ВЕРТИКЛЫ

A virtual thread verticle is just like a standard verticle but it’s executed using virtual threads, rather than using an event loop.

Virtual thread verticles are designed to use an async/await model with Vert.x futures.

Ага, просто DeploymentOptions deploymentOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
не канает!

НАдо  переписать код в async/await манере - https://vertx.io/docs/4.5.14/vertx-core/java/#virtual_threads
И поднять вертекс до последней версии 5.0.0
И 5.0.0 прям токает чтобы перейти на Future - не путать с жавовой Future
Вот теперь в тестах вертекса получается 20 сек на 100 повторов проотив 13 и 14 сек на платформенных потоков

VerticlePhilosophersTest (1): 100 total, 100 passed 20.53 s

И теперь переводим наши вертиклы в виртуальные потоки !

хм - 16-24-26 сек, как так? а, так мы не перешли в асинк/авейт парадигму!!!!
Переходим, визуально выглядит что решение на виртуальных потоках разгоняется и получаем 17 сек

Решил упростить тесты для вертекса: не буду разделять инит и логику кормления, код усложняется, лучше поднять  кол-во повторений чтобы поточнее посчитать
дельты. Кстати, по тестам хоршо видно тяжелые первые итерации, а потом тесты разгоняются.
Возможно придется соединить и для жава конкарренси тесты и бенчмарки чтобы битва была более честная. И кстати, это упростит код!

После выхода из тела теста вертекс стопается а это приводит в тому что работающие вертиклы ругаются исключениями. 
Для логики теста это не страшно, но тратим время на лог ошибок в консоль. Может стопнуть вертиклы перед выходом?

Так , теперь бенчмарки для вертекса: проблема в том что все выполняется асинхронно, и нам нужна индикация конца как для юниттестов.
Как этого добиться? Кажется прекрасно справится барьер или латч! И таки да!

Отступление - купил подписку на ультимейт и не смог ее применить даже с впн, это супер странно, НО допер скачать комьюнити
и получить поддержку последних java. Так и произошло. 

Теперь стоит перепрогнать мейны, тесты и бенчмерки и зафиксировать тайминги. Переписываем и упросщаем тесты и бенчмарки и мейны - слить
инит и кормление, для простоты и поднять кол-во прогонов. 

Отключаю логирование для вертекса чтоб побыстрее пролетали тесты и бенчмарки: когда стопается вертекс летят ошибки от еще работающих вертиклов, нам это не страшно

Бенчмарки для вертекса (200, 300 мс) пролетают сильно быстрее тестов (1.5 сек)- а нет ли каких то приколов с оптимизацией?

Тепреь когда новый код готов можно собрать финальную статистику для тестов и бенчмарков и решить
 - будем ли клепать суррогатное решение для ловли пининга, слипов, блоков и активного ожидания
 - будем ли клепать на вертексе решение для ловли пининга, слипов, блоков и активного ожидания

Benchmark                                                                                            Mode  Cnt    Score     Error  Units

Noop

UnitedPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_virtual_threads               avgt    5    1.952 ±   0.188  ms/op
UnitedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_virtual_threads                 avgt    5    2.184 ±   0.439  ms/op
UnitedPhilosophersBenchmark.test_synchronized_noop_philosophers_with_platform_threads                avgt    5  107.555 ±  19.364  ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_noop_philosophers_with_platform_threads              avgt    5  126.863 ±  24.680  ms/op

Очевидно виртуальные потоки рвут платформенные по скорости работы с блокировками и/или переключениями контекста на 2 порядка!
Различия в reentrant_lock и synchronized в пределах погрешности, несущественны

Thread.sleep()

UnitedPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_platform_threads          avgt    5  504.907 ±  28.813  ms/op
UnitedPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_platform_threads            avgt    5  804.849 ± 742.218  ms/op
UnitedPhilosophersBenchmark.test_synchronized_sleeping_philosophers_with_virtual_threads             avgt    5  893.064 ± 222.928  ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_sleeping_philosophers_with_virtual_threads           avgt    5  918.538 ± 147.244  ms/op

Результаты сомнительные, погрешности одного порядка со средним, 
перепроверка показывает что среднее для reentrant_lock на platform_threads немного лучше, результаты ложатся плотнее. 
Есть идея погонять на большем числе повторов (это сделаем потом)

InputStream.readAllBytes()

BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_platform_threads  avgt   10    585.786 ±   68.270  ms/op
BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_platform_threads    avgt   10    844.112 ±   41.460  ms/op
BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads   avgt   10   7490.787 ± 2143.440  ms/op
BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_virtual_threads     avgt   10  11868.748 ± 3361.973  ms/op

BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_platform_threads  avgt   10    575.464 ±   32.893  ms/op
BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_platform_threads    avgt   10    885.836 ±   62.762  ms/op
BlockingReadingPhilosophersBenchmark.test_reentrant_lock_blocking_reading_philosophers_with_virtual_threads   avgt   10   3972.985 ±  830.251  ms/op
BlockingReadingPhilosophersBenchmark.test_synchronized_blocking_reading_philosophers_with_virtual_threads     avgt   10  18758.470 ± 3171.521  ms/op

А вот это странное, платформенные потоки гораздо лучше читают файл??? Перепроверил. Так и есть, несмотря на погрешность
И кажется реентрант лок получше синхронайзда

ТУТ ТАКОЕ РЕЗЮМЕ - все сложено в одну корзинку, чтобы точно понять чо за нафиг - надо писать бенчмарк отдельно от философов на чтение и все остальное

while(System.nanoTime() > )

UnitedPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_virtual_threads     avgt    5  176.553 ±   4.187  ms/op
UnitedPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_virtual_threads       avgt    5  177.363 ±   9.932  ms/op
UnitedPhilosophersBenchmark.test_synchronized_active_waiting_philosophers_with_platform_threads      avgt    5  321.945 ±  53.122  ms/op
UnitedPhilosophersBenchmark.test_reentrant_lock_active_waiting_philosophers_with_platform_threads    avgt    5  386.645 ±  54.445  ms/op

Виртуальные потоки в 2 раза лучше активно ожидают чем платформенные?=) Нет конечно, все тот же контекст свитч и работа с блокировками
Лучше чем слипы более чем в 2 раза и хуже чем чтение файла на 1 порядок для платформенных и на 2 порядка для виртуальных - звучит логично

Vertx

UnitedPhilosophersBenchmark.test_virtual_noop_verticle_philosophers                                  avgt    5    204.009 ±   20.128  ms/op
UnitedPhilosophersBenchmark.test_verticle_noop_philosophers                                          avgt    5    287.172 ±   31.099  ms/op
UnitedPhilosophersBenchmark.test_virtual_active_waiting_verticle_philosophers                        avgt    5    838.508 ±  983.339  ms/op
UnitedPhilosophersBenchmark.test_virtual_blocking_reading_verticle_philosophers                      avgt    5   3328.586 ±   90.783  ms/op
UnitedPhilosophersBenchmark.test_virtual_sleeping_verticle_philosophers                              avgt    5   4239.687 ±  349.430  ms/op

Виртуальные вертиклы лучше классических, бегущих в event loop на четверть
На виртуальных вертиклах активное ожидание почти в 1,5 раза лучше блокирующего чтения, и в 4 раза лучше слипа
Активное ожидание сравнимо со слипом на классике, блокирующее чтение и слип хуже минимум в 2 раза. 
В целом проигрывают по производительности классике на 2 порядка, но не по удобству программной модели и простоте реализации.
Активное ожидание какое-то стремное...

--------------
--------------

Результаты интересные, надо докрутить для вертекса все варианты, но с учетом того что
 - ивент луп блокировать нельзя, блокирующее чтение и слипы запрещены - это надо делать либо в рабочих вертиклах либо в виртуальных, 
т е существующие стратегии constructXXXEating() по хорошему не подойдут для event loop вертиклов 
 - в вертексе есть асинхронное/неблокирующее чтение файла - надо решить насколько честно было использовать его
 - также можно через таймер красиво решить активное ожидание

Пока решаю что используем точно такие же constructXXXEating как и для классики.
Также решаю что реализую всех философов ХОТЯ это и нарушит правила для ивент лупа! Не, нафиг - вертекс страшно ругается в консоли, убираю
Можно задать вопрос в зал - как думаете , почему есть бенчмарки для вертиклов в ивент лупе для только noop? 

Тут интересно конечно погонять и 10К 100К 1КК философов и кормлений и посмотреть а что будет твориться на скейле для виртуальных потоков. Так что меня останавливает?

Помним что для платформеных потоков это не сработает. Исключаем их из бенчмарков. Оставляем только noop

Про нагрузку в философах - в принципе в исходном коде для хайлода было кормпление - случайное время внутри. 
В бенчмарках мы фактически это эмулируем.

Для вертиклов что-то идет не так, возвращаю логирование чтобы почитать исключения
Для вертиклов на ивент лупе что-то идет не так, за вменяемое время дождаться конца кормления не просто - исключаю из бенчмарка, оставляем битву между виртуальными потоками чисто
Для 1ККX1КК
Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "vertx-blocked-thread-checker"
Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "ru.spb.kupchinolab.jvmday2025.dining_philosophers._999_jmh_benchmarks_scaled.ScaledPhilosophersBenchmark.test_1000K_1000K_virtual_noop_verticle_philosophers-jmh-worker-1"
СТоит ли искать где может быть проблема? ИЛи просто решаем что есть затык во внутренней шине или shareddata? или в  черной дыре???
Если для других реализаций нет проблем с черной дырой, то мы ее обнаружим

Возвращаю бенчмарк для не обычных вертиклов, т к они по идее могут поддержать такое кол-во, жаль не получить данные и не сравнить

ПОднимаю кол-во прогревов с 1 до 3 т к разница во времени прогрева и боевых итераций различается на порядок и иногда первая итерация тоже сильно улетает

Для test_0010K_1000K_noop_verticle_philosophers

Exception: java.lang.OutOfMemoryError thrown from the UncaughtExceptionHandler in thread "vertx-blocked-thread-checker"

Надо унести все 1KK в конец файла - jmh исполняет в лексикографическом порядке

Оказывается форки остаются висеть
JMH had finished, but forked VM did not exit, are there stray running threads? Waiting 9 seconds more...
Кажется есть идея почему так может произойти - все таки важно дождаться окончания вертекса на клозе т к бенчмарк бежит в форкнутом процессе и там и остается!
Тест
Benchmark                                                          Mode  Cnt    Score    Error  Units
NoopVerticlePhilosophersBenchmark.test_verticle_noop_philosophers  avgt    5  262.002 ± 91.606  ms/op
против с правильным клозом
Benchmark                                                          Mode  Cnt    Score    Error  Units
NoopVerticlePhilosophersBenchmark.test_verticle_noop_philosophers  avgt    5  265.028 ± 72.609  ms/op

Вроде ни на что нее влияет но теперь правильно! 
Теперб надо перепрогнать [UnitedPhilosophersBenchmark.java](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_099_jmh_benchmarks_united/UnitedPhilosophersBenchmark.java)
И сверить с прошлыми замерами
Так, [UnitedPhilosophersBenchmark.java](src/main/java/ru/spb/kupchinolab/jvmday2025/dining_philosophers/_099_jmh_benchmarks_united/UnitedPhilosophersBenchmark.java)
залипает на test_virtual_blocking_reading_verticle_philosophers - кормление заканчивается, а бенчмарк нет
Отдельный бенчмарк доказывает чот BlockingReadingVirtualVerticlePhilosophersBenchmark залипает
Почему?
Включаю логирование в вертекс. Дописываю еще бенчмарк для активного ожиджания, может быть проблемма конкретно в сочетании виртуального вертикла и блокирующего чтения файла;
Таки да, на активном чтении неит проблем с остановкой виртуальных вертиклов
Уже до кучи проверим слип
Для слипа - все хорошо, т е проблма конкретно с блокирующим чтением и виртуальным вертиклом. Но почему??
Идея такая - не может высвободить ресурсы. Какие? Файлы! Но там автоклозабл, но мы можем стопать вертиклы когда они берут файл на чтение, и тем не менее автоклозабл!
Ой, java.io.FileNotFoundException: /Users/vladimir/Development/bob-the-dyer/jvm-day-2025/16KB_file.txt (No such file or directory)
Т е все это время мы ничего не читали потому что ошиблись именем файла????!!!!
И все "бенчмарки на смарку"
Ну что ж - переделаем!
Переделал, стало интереснее, обновил результаты, хорошо бы запилить суррогатный бенчмарк без палочек, точнее с одной палочкой!
