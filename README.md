BLAZE: A developent build system.

{Fast | Correct} choose two.


Building with Maven feels like making a battleship turn. Its so heavy and slowww.
The jvm loads, mavens initializes plugins, and checks a hundred other things most of which stay the same 
around 99% of the time. Even the smallest of projects take a couple of seconds to build.
For medium sized projects, typically it takes a minute to do a build. Deploying a web application 
on tomcat etc is another 30 seconds or so, to unpack the war files, load the app in memory etc.
Anyway you get the picture. This is enough for the developer to lose focus and context and take another 5 minutes to 
get back in the groove. Very frustrating.

why does it take around a minute to see the changes I made in a couple of files?

Introducing Blaze!

The goal is to get SUBSECOND incremental builds, in places where maven + tomcat takes minutes.

Here's how blaze does it.

-- Once started, blaze becomes a "build server". So you request a rebuild through a client. No more wasting a 
couple of seconds bringing up the jvm
-- It maintains a dependency graph of class files in memory. We use the ASM bytecode framework to calculate class level dependencies and recompile precisely what needs recompiling. 
-- Never jar anything. jars are the bane of development. The whole packing unpacking thing is simply an overhead for development.
-- Runs an integrated jetty server for reloading. So no time lost in startup and shutdown
-- Another huge optimization is that there is a classloader in the app which loads jetty initially with the project classpath, EXCEPT for the project files themselves. There is a webapplication context which contain solely the classfiles of the application. Also, through a lot of classloading magic, We can restart only the application in code. This makes it blazing fast.

The correctness is ensured because, During the first run, it uses maven to obtain the initial classpaths, etc.

The net result is subsecond recompiles. allowing python like development in java! 

Enjoy!
