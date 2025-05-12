# TODO тут будет название доклада

## Описание
TODO тут будет описание доклада

## Кому может быть интересно
TODO

## Мотивация к появлению данного доклада
TODO
 
## TODO Ссылка на доклад на хайлоде

## TODO Минимум знаний, необходимый для понимания:
 - concurrency - synchronized, ReentrantLock, CountDownLatch, Structured Concurrency
 - https://vertx.io (если войдет в доклад)
 
## TODO Что разбирается в проекте

Тут надо определить какие именно феномены хочется продемонстрировать и мифы развенчать

Претенднты
 - очевидный - кол-во потоков 10К, 1М
 - switch context
 - blocking (on IO)

Формат разбора задач: 
 1. постановка задачи
 1. решение synchronized
 1. решение ReentrantLock
 1. (под вопросом) решение на vert.x без блокировок
 1. сравнение производиельности, результаты JMH  
 1. масштабирование раз  
 1. масштабирование два  
 1. сравнительный анализ
 1. добавляем виртуальности во все и все заново
 1. общее сравнение

## TODO Итоги выводы

## Ссылки
https://wiki.openjdk.org/display/loom/Main
https://openjdk.org/jeps/425
https://openjdk.org/jeps/453
https://en.wikipedia.org/wiki/Dining_philosophers_problem
https://vertx.io

Virtual threads are not faster threads — they do not run code any faster than platform threads. They exist to provide scale (higher throughput), not speed (lower latency). There can be many more of them than platform threads, so they enable the higher concurrency needed for higher throughput according to Little's Law.

To put it another way, virtual threads can significantly improve application throughput when
 - The number of concurrent tasks is high (more than a few thousand), and
 - The workload is not CPU-bound, since having many more threads than processor cores cannot improve throughput in that case.

virtual threads are not cooperative.




Typically, a virtual thread will unmount when it blocks on I/O or some other blocking operation in the JDK, such as BlockingQueue.take(). When the blocking operation is ready to complete (e.g., bytes have been received on a socket), it submits the virtual thread back to the scheduler, which will mount the virtual thread on a carrier to resume execution.

The vast majority of blocking operations in the JDK will unmount the virtual thread, freeing its carrier and the underlying OS thread to take on new work. However, some blocking operations in the JDK do not unmount the virtual thread, and thus block both its carrier and the underlying OS thread. This is because of limitations either at the OS level (e.g., many filesystem operations) or at the JDK level (e.g., Object.wait())


There are two scenarios in which a virtual thread cannot be unmounted during blocking operations because it is pinned to its carrier:

When it executes code inside a synchronized block or method, or
When it executes a native method or a foreign function.

The stacks of virtual threads are stored in Java's garbage-collected heap as stack chunk objects.

Unlike platform thread stacks, virtual thread stacks are not GC roots, so the references contained in them are not traversed in a stop-the-world pause by garbage collectors, such as G1, that perform concurrent heap scanning. This also means that if a virtual thread is blocked on, e.g., BlockingQueue.take(), and no other thread can obtain a reference to either the virtual thread or the queue, then the thread can be garbage collected — which is fine, since the virtual thread can never be interrupted or unblocked. Of course, the virtual thread will not be garbage collected if it is running or if it is blocked and could ever be unblocked.




Начало

Навеяно spring.threads.virtual.enabled=true и все стало быстрее или не стало

Я делал доклад. Смотрите как там все было

И в подумал а что будет если просто взять и поменять потоки на виртуальные например на философах

Взял старый код. упростил его до предела и запустил и .. неповерил

Какая то проблема с остановкой потоков через exit или Timer?
Неужели атомик внутри виртуальных потоков что делает что становится хуже платформенных потоков?
ВИртуальные хуже, по производительности, но платформенных 10К не создать
Кстати, а как узнать сколько платформенных потоков можно создать неэмпирически?

Нет, кажется мы реально просто не успеваем создать все потоки - проверим - вернул барьер
Может проблема в jdk? Меняем с либерики на оракл, с платформенными потоками тот же рез (24,0,1) а с виртуальными в 2 раза лучше!!!
Вот это да, может проблема в полной версии - качаю не полную версию либерики - результат лучше чем у полной, хуже чем у оракловой
Ну и контролный выстрел в голову - проверить чо как на ea openjdk 25й - не завелось в IDE : it is configured to use JDK 0, but IDE supports compilation using JDK 7 and newer only
Итого - ничего не понятно, пару раз на фул тоже показало норм результат, еще залипает барьер, это совсем нехорошо - РАЗОБРАТЬСЯ БЫ ПОЧСЕПУ, все равно всплывет на тестах jmh
Переключаюсь на опен ждк
Какие тут возможны выводы - взятие и отпускание блокировок происходит эффективнее, т к пдатформенный поток не уходит в блок
Но важно отметить, что виртуальные потоки НЕ для этого были сделаны, а как раз наоброт для некооперативного взаимодействия

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

Для нескольких jdk, для нескольких количеств, окружение будет локальным, но максимально одинаковым 

Интересно, а есть ли разница с синхронайзом? 
Пишем точно такой же код, тестируем с jmh

Ладно, выяснили что с виртуальными потоками мы можем держать огромное кол-во некооперирующих потоков - хорошо для промышленных стандартных задач! а именно обработка запросов на вебсервере

Но это не покажет нам пининг, так как внутри нет блокирующего метода, так давайте же сделаем - урл(-)? очередь(?)?
Не понял что не так с барьером и вернулся к латчу
на миллионе хорошо видно что потоки что-то пытаются сделать но  кол-во попыток пришло к 1К потому что все остальное время происходит контеншен за палочки

Оценку доступных платформенных потоков через Thread.activeCount()
Для 5 тыс фил всего 3 платформенных потока!
На моей тачке для пользовательского жава процесса 4060+ примерно платформенных потоков
2,6 GHz 6-Core Intel Core i7  16 GB 
