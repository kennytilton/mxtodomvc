# TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*

The `Matrix` dataflow library endows application state with causal power over other such state, freeing the developer from the burden of propagating unpredictable change across highly interdependent models. It does so working at the fundamental level of reading and writing properties, and by providing a precise mechanism for applications to act on the world, if only to update a computer screen.

> ma·trix ˈmātriks *noun* an environment in which something else takes form. *Origin:* Latin, female animal used for breeding, parent plant, from *matr-*, *mater*

More grandly, Matrix brings our application models to life, animating them in response to streams of external inputs. The movies were fun, but that Matrix sucked energy from humans to feed machines. Mr. Hickey, a careful man with the dictionary, might disapprove the misconstruction.

*You say "reactive", we say "dataflow"*

Most today call this _reactive programming_. That describes well the programmer mindset in the small, but we find _dataflow_ more descriptive of the emergent systems.

*Prior and concurrent art*

Matrix enjoys much good company in this field. We believe Matrix offers more simplicity, transparency, granularity, expressiveness, efficiency, and functional coverage, but in each dimension differs in degree, not spirit. Other recommended CLJS libraries are Reagent, Hoplon/Javelin, and re-frame. Beyond CLJS, we admire MobX (JS), binding.Scala, and Python Trellis.

*mxWeb, "poster" application*

`mxWeb` is a thin web un-framework built atop Matrix. We introduce Matrix in the context of mxWeb because nothing challenges a developer more than keeping application state straight while an intelligent user does their best to use a rich interface correctly. Then marketing wants the U/X redone.

We say "un-framework" because mxWeb exists only to wire the DOM for dataflow. The API design imperative is that the MDN reference be the mxWeb reference; mxWeb itself introduces no new architecture.

#### TodoMVC
So far, so abstract. Ourselves, we think better in concrete. Let's get "hello, Matrix" running and then start building TodoMVC from scratch. 

The TodoMVC project specifies a trivial Web application as the basis for comparing Web frameworks. We will first satisfy the requirements, then extend the spec to include XHRs. Along the way we will tag milestones so the reader can conveniently visit any stage of development.

We begin.

## Set-Up

````bash
git clone https://github.com/kennytilton/mxtodomvc.git
cd mxtodomvc
lein deps
lein clean
lein fig:build
````
This will auto compile and send all changes to the browser without the need to reload. After the compilation process is complete, you will get a Browser Connected REPL. A web page should appear on a browser near you with a header saying just "hello, Matrix". 

For issues, questions, or comments, ping us at kentilton on gmail, or DM @hiskennyness on Slack in the #Clojurians channel.

## Building TodoMVC from Scratch
When starting on a TodoMVC implementation, we execute first just the title and footer as our own little "hello, world". Let us jump now to the commit of that milestone:
````bash
git checkout title-and-credits
````
From now on, our cue to check out a new tag will be these headers:
#### checkout tag: title-and-credits
And here is the mxWeb HTML work-aike, look-alike code:
````clojure
(defn matrix-build []
  (md/make
    :mx-dom (section {:class "todoapp"}
              (header {:class "header"}
                (h1 "todos")
                (mxtodo-credits)))))
````
The "tags" such as `header` and `h` are CLJS macros. It is *all* CLJS.

As with HTML, each mxWeb tag macro takes the same parameters:
* an optional map of DOM attributes;
* an optional map of custom application properties; and
* any number of child elements.

The sharp-eyed reader has spotted an unlikely HTML tag, `mxtodo-credits`. Here is the code for that:
````clojure
(defn mxtodo-credits []
  (footer {:class "info"}
    (for [credit ["Double-click a to-do list item to edit it."
                  "Created by <a href=\"https://github.com/kennytilton\">Kenneth Tilton</a>."
                  "Inspired by <a href=\"https://github.com/tastejs/todomvc/blob/master/app-spec.md\">TodoMVC</a>."]]
      (p credit))))
````
We now effectively have [Web Components](https://developer.mozilla.org/en-US/docs/Web/Web_Components). `mxtodo-credits` is rather simple, but another function could take as many parameters as necessary to be reusable.

Note also that, yes, we can mix standard CLJS with our "HTML" because, again, it is all CLJS.
### checkout tag: wall-clock
Reminder:
````bash
git checkout wall-clock
````
The TodoMVC spec does not include a time or date display, but adding now a "wall clock" needed later for extensions to the spec lets us learn more about Matrix faster. The wall clock component demonstrates:
1. automatic state management: our first dataflow;
1. transparent state management;
2. DOM efficiency without VDOM complexity;
3. the mxWeb approach to Web Components;
4. all dataflow all the time: "lifting" components into the Matrix;
5. a single source of behavior: co-location of model and view; and
6. the Grail of object reuse.

And now the code. First, the big picture illustrating mxWeb's approach to "Web Components":
````clojure
(defn matrix-build []
  (md/make
    :mx-dom (section {:class "todoapp"}
              (wall-clock :date 60000 0 15)
              (wall-clock :time 1000 0 8)
              (header {:class "header"}
                (h1 "todos?")
                (mxtodo-credits)))))
````
One `wallclock` shows the date and updates every hour [no, this makes no sense], the other shows the time second by second. And now the component:
````clojure
(defn wall-clock [mode interval start end]
  (div
    {:class "std-clock"}
    {:clock  (cI (util/now))
     :ticker (cFonce
               (js/setInterval
                 #(mset!> me :clock (util/now))
                 interval))}
    (as-> (<mget me :clock) date
      (js/Date. date)
      (case mode
        :time (.toTimeString date)
        :date (.toDateString date))
      (subs date start end))))
````
If you prefer, check out the actual source. It is heavily commented with everything we will say here. Now let's work through the features above one by one.
#### 1. automatic state management: our first dataflow  
On every interval, the imperative `mset!>` feeds the browser clock epoch into the application Matrix `clock` property. The child string content of the DIV gets regenerated because `clock` changed. In code we will learn about later, mxWeb knows to reset the innerHTML of the DOM element corresponding to our proxy DIV. Hello, dataflow.
#### 2. transparent state management
There is no explicit publish or subscribe. We simply read with `mget` and assign with `mset!>`. When we get to managing Todo items, we will hide mget/mset!> behind functions. (Dependency tracking sees into function calls.)
#### 3. DOM efficiency without VDOM cost and complexity . 
The preceding explains why mxWeb is faster than VDOM; property-to-property dataflow means the system knows with fine granularity when and what DOM needs updating when new inputs hit the Matrix. The actual code includes strategically placed print statements that illustrate in the console that the DIV is created once but its content on each interval. This is a small win, but in examples to come we achieve significant changes with no more than `classlist/set`.
#### 4. the mxWeb approach to Web Components . 
Above we see the function `wall-clock` has four parameters, `[mode interval start end]`. Achieving component reuse with mxWeb differs not at all from parameterizing any Clojure function for maximum utility.
#### 5. all dataflow all the time: "lifting" components into the Matrix  
Browsers do not know about the Matrix dataflow library, so we have to write more or less glue code to bring them into the datafow.  
````clojure
(js/setInterval
    #(mset!> me :clock (util/now))
    interval)
````  
We call this gluing process "lifting". Lifting the system clock required just a few lines of code. We hinted earlier that mxWeb exemplifies "lifting". That took almost two thousand lines. Because dataflow.
#### 6. a single source of behavior: co-location of model and view  
This may be an anti-feature to many. Our wall clock widget needs application state, and it generates and relays that state itself. The `clock` property holds the JS epoch, and the 'ticker' property holds a timer driving `clock`. Nearby in the code, a child element consumes the stream of `clock` values. Everything resides together in the source for quick authoring, debugging, revision, and understanding.
> The current trend in web library architecture involves decomposing monolithic apps into small elements combined usefully at run-time by the library to form the desired application. With mxWeb, the elements shaping an application behavior are found together in the source. Bucking trends makes us nervous, so we were happy to see Facebook engineers bragging on their "co-location" of GraphQL snippets alongside the components that consumed them.  
#### 7. the Grail of object reuse  
In classic OOP, objects have rigid definitions making generality unlikely. DIV elements do not generally need a stream of clock values, so normally we would need to sub-class DIV to arrange for one, or wire up access to a stream maintained elsewhere. Matrix works like the prototype model of OOP; we can code up a new dataflow-capable clock property on the fly.

## License

Copyright © 2018 Kenneth Tilton

Distributed under the MIT License.
