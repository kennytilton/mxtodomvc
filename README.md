# TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*

## tl;dr
mxWeb makes web pages easier to build, debug, refactor, and maintain simply by changing what happens when we read and write properties:
* when B reads A, A remembers B; and
* when A changes, A tells B.

From that we get:
* declarative/functional code everywhere (not just the component);
* more efficiency than is possible with VDOM; and
* no other framework to learn.

What does it mean for B to read A? It means B is expressed as an HLL function that reads A. An mxWeb "HTML" excerpt from the code below:
````clojure
(li
    {:class (cF (when (<mget todo :completed)
                  "completed"))}
    ...)
````
...and another:
````clojure
(md/make ::todo-list
    :items-raw (cI nil)
    :items (cF (doall (remove td-deleted (<mget me :items-raw))))
    :items-completed (cF (doall (filter td-completed (<mget me :items))))
    :items-active (cF (doall (remove td-completed (<mget me :items))))
    :empty? (cF (empty? (<mget me :items))))
````
Glossary for close readers:
* `cI`, or "input Cell", can be written to by imeprative code;
* `cF`, or "cell formulaic", makes the `:class` property of the `LI` functional, using the body shown; and
* `<mget`, "or model get", is the Matrix property reader that arranges for `:completed` to remember `:class`.

What does it mean for A to change and then tell B? It means we imperatively change A using a special Matrix writer, and then Matrix internals recalculate B:
````cljs
(input {:class       "toggle"
        ::mxweb/type "checkbox"
        :onclick     #(mswap!> todo :completed
                         #(when-not [%] (util/now)})
````
`mswap!>` is a Matrix property writer that:
* changes the `:completed` property of the model todo; and
* before returning, recomputes the :class property.

One step is missing. How does the `li` DOM classlist change? We held something back:
* when `A` changes, it can:
    * mutate properties outside the Matrix graph; or
    * enqueue Matrix writes to other properties for execution immediately after the current write
* 
````clojure
(defmethod observe-by-type
  [:mxweb.base/tag]
  [slot me newv oldv _]
  (when-let [dom (tag-dom me)]
      (case slot
        :class (classlist/set dom
                 (if (sequential? newv)
                   (str/join " " newv)
                   newv))))))
````
Glossary for close readers:
* `observe-by-type` is one function from the overall "observer" mechanism dispatched when a property changes;
    * we use "observer" in the strict dictionary sense of "monitor, not participant";
* `tag-dom` returns the DOM element corresponding to an mxWeb proxy tag instance; and
    * the name "tag" comes from the HTML usage;
* `slot` is a Common Lisp holdover for property;
* `me` is like `this` or `self`;
* `classlist/set` is from `goog.dom`

That is the mxWeb framework. But here are some addenda (for close readers):
* We covered B reading A, but what if C reads B?  
B will remember C, and when A changes and has B change, B will have C change.
* The typical application Matrix is a tree of so-called *models* (objects)  
A Matrix observer on the special property `:kids` brings dynamically computed models into and out of the Matrix smoothly.

## The Full Story
The *Matrix* dataflow library endows application state with causal power, freeing us of the burden of propagating change across highly interdependent models. More grandly, it brings our application models to life, animating them in response to streams of external inputs.
> "UIs...I am not going to go there. I don't do that part."  
-- Rich Hickey on the high ratio of code to logic in UIs, *Clojure/Conj 2017*

We choose mxWeb as the vehicle for introducing Matrix because nothing challenges a developer more than keeping application state straight while an intelligent user does their best to use a rich interface correctly. Then marketing wants a U/X overhaul.

*mxWeb* is a thin web un-framework built atop Matrix. We say "un-framework" because mxWeb exists only to wire the DOM for dataflow. The API design imperative is that the MDN reference be the mxWeb reference; mxWeb itself introduces no new architecture.

Matrix does this simply by enhancing how we initialize, read, and write individual properties:
* properties can be initialized as a literal value or as a function;
* should some property `A` be initialized with a literal, we can write to it;
* should a functional property `B` read `A`, `A` remembers `B`;
* when we write to `A`, `A` tells `B`; and
* we can supply "on change" callbacks for properties.

What does it mean for one property to read another, for `B` to read `A`? It means declaring `B` as an arbitrary HLL function of `A` and possibly others. In pseudo code:
````clojure
B <= (fn [] (+ 42 A C))))
````
What does it mean for `A` to tell `B`? `A` makes `B` compute a new value. 

What happens when `B` computes a new value?
* `B` might have its own dependent properties to tell; and
* `A` or `B` might also want to act on the world outside the graph of properties. 

> "Nothing messes with functional purity quite like the need for side effects. On the other hand, effects are marvelous because they move the app forward." - [re-frame intro](https://github.com/Day8/re-frame)

A Web game app might use a CLJS map to model a Romulan warship, and have a paired DOM element to render the ship. If `A` is the `cloaked` property of the map warship and it changes, the `hidden` attribute of the DOM warship needs to be added or removed. To this end, Matrix lets us define "on-change" *observers*.

> [observer](https://dictionary.cambridge.org/dictionary/english/observer): noun. UK: /əbˈzɜː.vər/, US: /əbˈzɝː.vɚ/  A person who watches what happens but has no active part in it.

Observers are *monitors* of the dataflow between a graph of properties, not participants in that flow. They act, but they act outside the dataflow graph. *Caveat lector*: the reactive community generally uses "observer"...differently.

#### lifting
What about X, Y, and Z? i.e., Properties from existing libraries that know nothing about dataflow? We write whatever "glue" code it takes to wire existing libraries with dataflow. We call this "lifting" libraries into the dataflow. 

> Lifting the DOM required about two thousand lines of code. Below we will explore several examples of lifting. 

#### matrix?
`A` might be more than a descriptive property such as "cloaked". `A` might be `K` for "kids" and hold the child nodes of some parent; i.e., the very population of our application model can shrink or grow with events. We call such a dynamic population of communicating nodes a *matrix*.

> ma·trix ˈmātriks *noun* an environment in which something else takes form. *Origin:* Latin, female animal used for breeding, parent plant, from *matr-*, *mater*

Simply by propagating change between functional properties, with strictly segregated dataflow to and from the outside world, the Matrix library brings applications to life.

#### Really?
Can we really program this way? This [Algebra](https://tiltonsalgebra.com/#) app consists of about twelve hundred `A`s and `B`s, and extends into a Postgres database. Everything runs under matrix control. It lifts Qooxdoo JS, MathJax, Postgres and more. The average number of dependencies for one value is a little more than one, and the deepest dependency chain is about a dozen. On complex dispays of many math problems, a little over a thousand values are dependent on other values.

#### Related work
> "Derived Values, Flowing" -- the [re-frame](https://github.com/Day8/re-frame/blob/master/README.md) tag-line

Matrix enjoys much good company in this field. We believe Matrix offers more simplicity, transparency, granularity, expressiveness, efficiency, and functional coverage, but in each dimension differs only in degree, not spirit. Other recommended CLJS libraries are [Reagent](https://reagent-project.github.io/), [Hoplon/Javelin](https://github.com/hoplon/javelin), and [re-frame](https://github.com/Day8/re-frame). Beyond CLJS, we admire [MobX](https://github.com/mobxjs/mobx/blob/master/README.md) (JS), [binding.Scala](https://github.com/ThoughtWorksInc/Binding.scala/blob/11.0.x/README.md), and Python [Trellis](https://pypi.org/project/Trellis/). Let us know about any we missed.

#### TodoMVC
So far, so abstract. Ourselves, we think better in concrete. Let's get "hello, Matrix" running and then start building [TodoMVC](http://todomvc.com) from scratch. 

The TodoMVC project specifies a trivial Web application as the basis for comparing Web frameworks. We will first satisfy the requirements, then extend the spec to include XHRs. Along the way we will tag milestones so the reader can conveniently visit any stage of development.

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
git checkout hello-todomx
````
From now on, our cue to check out a new tag will be these headers:
#### git checkout hello-todomx
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
The above illustrates that, by supporting arbitrary functions as generators of HTML, with mxWeb we can develop custom HTML tags wrapping arbitrarily complex, reusable native DOM clusters, aka [Web Components](https://developer.mozilla.org/en-US/docs/Web/Web_Components). `mxtodo-credits` is rather simple, but next up is a function/component taking four parameters to support reuse.

Note also that, yes, we can mix standard CLJS with our "HTML" because, again, it is all CLJS.
### git checkout wall-clock
Reminder:
````bash
git checkout wall-clock
````
The TodoMVC spec does not include a time or date display, but a simple "wall clock" permits a quick but deep dive into:
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
The first `wall-clock` shows the date and updates every hour (no, this makes no sense), the second shows the time second by second. And now the component:
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
#### 1. automatic state management: our first dataflow  
> "Any component that uses an atom is automagically re-rendered when its value changes." -- [Reagent](https://reagent-project.github.io/)

> "Anything that can be derived from the application state, should be derived. Automatically." -- the [MobX](https://github.com/mobxjs/mobx) philosophy

On every interval, the imperative `mset!>` feeds the browser clock epoch into the application Matrix `clock` property. The child string content of the DIV gets regenerated because `clock` changed. In code we will learn about later, mxWeb knows to reset the innerHTML of the DOM element corresponding to our proxy DIV. Hello, dataflow.

> "Cells automatically and consistently propagate data dependency changes" -- [Hoplon/Javelin](https://github.com/hoplon/hoplon/wiki/Hoplon-Overview)
#### 2. transparent state management
> "Instead of worrying about subscribing or “listening” to events and managing the order of callbacks, you just write rules to compute values." -- Python [Trellis](https://pypi.org/project/Trellis/)

There is no explicit publish or subscribe. We simply read with `mget` and assign with `mset!>`. When we get to managing Todo items, we will hide mget/mset!> behind functions. (Dependency tracking sees into function calls.)
#### 3. DOM efficiency without VDOM cost and complexity
> "When part of the data source changes, Binding.scala knows about the exact corresponding partial DOM affected by the change." -- binding.Scala [design](https://github.com/ThoughtWorksInc/Binding.scala)

The preceding explains why mxWeb is faster than VDOM; property-to-property dataflow means the system knows with fine granularity when and what DOM needs updating when new inputs hit the Matrix. The actual code includes strategically placed print statements that illustrate in the console that the DIV is created once but its content on each interval. This is a small win, but in examples to come we achieve significant changes with no more than `classlist/set`.
#### 4. the mxWeb approach to Web Components
Above we see the function `wall-clock` has four parameters, `[mode interval start end]`. Achieving component reuse with mxWeb differs not at all from parameterizing any Clojure function for maximum utility.
#### 5. all dataflow all the time: "lifting" components into the Matrix  
We want to program with it as much as possible but, as just one example, browsers do not know about Matrix dataflow. If the component will not come to the Matrix, the Matrix will wrap the component: we write more or less "glue" code to bring it into the datafow.  
````clojure
(js/setInterval
    #(mset!> me :clock (util/now))
    interval)
````  
We call this gluing process "lifting". Lifting the system clock required just a few lines of code. We hinted earlier that mxWeb exemplifies "lifting". That took almost two thousand lines.
#### 6. a single source of behavior: co-location of model and view  
This will be an anti-feature to many. Our wall clock widget needs application state, and it generates and relays that state itself. The `clock` property holds the JS epoch, and the 'ticker' property holds a timer driving `clock`. Nearby in the code, a child element consumes the stream of `clock` values. Everything resides together in the source for quick authoring, debugging, revision, and understanding.
> The current trend in web library architecture involves decomposing monolithic apps into small elements combined usefully at run-time by the library to form the desired application. With mxWeb, the elements shaping an application behavior are found together in the source. Bucking trends makes us nervous, so we were happy to see Facebook engineers bragging on their "co-location" of GraphQL snippets alongside the components that consumed them.  
#### 7. the Grail of object reuse  
In classic OOP, objects have rigid definitions making generality unlikely. Few DIV elements need a stream of clock values, so normally we would need to sub-class DIV to arrange for one. Matrix, like the prototype model of OOP, lets us code up a new dataflow-capable clock property on the fly.

### git checkout enter-todos
As promised, that was a deep first dive. This tag will be much simpler, merely introducing to-dos, unstored, unedited, and not even enterable:
* we load a few fixed-todos at start-up;
* we show them in a list;
* one control lets us toggle a to-do between completed or not; and
* another control deletes a to-do, logically but irrevocably.

Here is how we make a to-do. Recall that `cI` is shorthand for making an "input cell", one to which we can assign new values from imperative code in an event handler.
````clojure
(defn make-todo
  "Make a matrix incarnation of a todo item"
  [title]

  (md/make
    :id (util/uuidv4)
    :created (util/now)
    :title (cI title)
    :completed (cI nil)
    :deleted (cI nil)))
````
The only slots demanded by the spec are "title" and a boolean "completed", but as experienced CRUD developers we went further in ways not important to this exercise. Note that apparent booleans will in fact be nil or timestamps, so no "?" suffixes.

Here is the to-do-list container model, set up to take a list of hard-coded initial to-dos to get us rolling:
````clojure
(defn todo-list [seed-todos]
  (md/make ::todo-list
    :items-raw (cFn (for [to-do seed-todos]
                      (make-todo to-do})))
    :items (cF (doall (remove td-deleted (<mget me :items-raw))))))
````
The bulk of the to-do app does not care about deleted to-dos, so we use a clumsy name "items-raw" for the true list of items ever created, and save the name "items" for the ones actually used.

We can now start our demo matrix off with a few preset to-dos. Some things to note:
* the optional first "type" parameter ::todoApp is supplied
* building the matrix DOM is now wrapped in `(cFonce (with-par me ...)`;
  * `cFonce` effectively defers the enclosed form until the right lifecycle point in the matrix's initial construction.
  * `with-par me` is how the matrix DOM knows where it is in the matrix tree. All matrix nodes know their parents so they can navigate the tree freely to gather information.
* the app credits are now provided by a new "web component", and that along with the "wall-clock" reusable are off in their own namespace.
* most interesting is `(mxu-find-type me ::todoApp)`, a bit of exposed wiring that demonstrates how Matrix elements pull information from elsewhere in the Matrix using various "mx-find-\*" selector-like utilities we will discuss below. 

````clojure
(md/make ::md/todoApp
      :todos (todo/todo-list ["Wash car" "Walk dog" "Do laundry" "Mow lawn"])
      :mx-dom (cFonce
                (with-par me
                  (section {:class "todoapp" :style "padding:24px"}
                    (webco/wall-clock :date 60000 0 15)
                    (webco/wall-clock :time 1000 0 8)
                    (header {:class "header"}
                      (h1 "todos"))
                    (todo-items-list)
                    (todo-items-dashboard)
                    (webco/app-credits mxtodo-credits)))))
````
And now the to-do item view itself, the structure and nice CSS authored by the developers of the TodoMVC exercise.
````clojure
(defn todo-list-item [todo]
  (li
    {:class (cF (when (td-completed todo)
                  "completed"))}
    {:todo todo}
    (div {:class "view"}
      (input {:class       "toggle"
              ::mxweb/type "checkbox"
              :checked     (cF (not (nil? (td-completed todo))))
              :onclick     #(td-toggle-completed! todo)})

      (label (td-title todo))

      (button {:class   "destroy"
               :onclick #(td-delete! todo)}))))
````
Elsewhere we find the "change" dataflow initiators:
````clojure
(defn td-delete! [td]
  (mset!> td :deleted (util/now)))

(defn td-toggle-completed! [td]
  (mset!> td :completed
    (when-not (td-completed td) (util/now))))
````
We may as well get what we call the dashboard out of the way (without the selectors if you know your TodoMVC) because it adds a lot of behavior without any more Matrix complexity:
````clojure
(defn todo-items-dashboard []
  (footer {:class  "footer"
           :hidden (cF (<mget (mx-todos me) :empty?))}
    (span {:class   "todo-count"
           :content (cF (pp/cl-format nil "<strong>~a</strong>  item~:P remaining"
                          (count (remove td-completed (mx-todo-items me)))))})
    (button {:class   "clear-completed"
             :hidden  (cF (empty? (<mget (mx-todos me) :items-completed)))
             :onclick #(doseq [td (filter td-completed (mx-todo-items))]
                         (td-delete! td))}
      "Clear completed")))
````
In support of the above we extend the model of the to-do list with more dataflow properties:
````clojure
(defn todo-list [seed-todos]
  (md/make ::todo-list
    :items-raw (cFn (for [td seed-todos]
                      (make-todo td)))
    :items (cF (doall (remove td-deleted (<mget me :items-raw))))
    :items-completed (cF (doall (filter td-completed (<mget me :items))))
    :empty? (cF (empty? (<mget me :items)))))
````
Other things the reader might notice:
* `mx-todos` and `mx-todo-items` wrap the complexity of navigating the Matrix to find desired data;
* `doall` in various formulas may soon be baked in to Matrix, because lazy evaluation breaks dependency tracking.  
Recall that Matrix works by changing what happens when we read properties. The internal mechanism is to bind a formula to `*depender*` when kicking off its rule. With lazy evaluation, that binding is gone by the time the read occurs.

We can now play with toggling the completion state of to-dos, deleting them directly, or deleting them with the "clear completed" button, keeping an eye on "items remaining". Consider now what happens when we click the completion toggle of the view displaying an uncompleted todo:
* the completed property of the associated model gets set to the current JS epoch;
* the class of the todo list item view gets recomputed because it read that :completed property. It changes to "completed";
* the LI DOM element classList gets set to "completed" by an mxWeb observer;
* the :content property formula of the "Items remaining" span recounts the list of todos filtering out the completed and comes up with one less;
* an mxWeb observer updates the span innerHTML to the new "remaining" count;
* the :items-completed property on the todos list model container gets recalculated because it reads the :completed property of *all* todo item models. It grows by one;
* the :hidden property of the "Clear completed" button/map gets recalculated because it reads the :items-completed property of the list of todos. If the length changes either way between zero and one, the :class property gains or loses the "hidden" value and...
* an mxWeb observer updates the classList of the DOM element corresponding to the "Clear completed" button.

All that happens when this code executes:
````clojure
(mset!> td :deleted (now))
````
We will spare the reader our detailed analysis of what happens when we click the "delete" button (the red "X" that appears on hover), but the reader might want to work out for themselves the dataflow from the :deleted property to these behaviors:
* the item disappears;
* if the item was incomplete when deleted, the "Items remaining" drops by one;
* if the item was the only completed item, "Clear completed" disappears;
* if the item was the last of any kind, the dashboard disappears.

Next up: the spec requires a bit of routing.

#### git checkout lift-routing
The official TodoMVC spec requires a routing mechanism be used to implement the user filtering of which to-dos are displayed, based on their completion status. The options are "all", "completed" only, and "active" (incomplete) active only.

The declarative code just reads the route, a property of the root node of the matrix:
````clojure
(defn todo-items-list []
  (section {:class "main"}
    (ul {:class "todo-list"}
      (for [todo (sort-by td-created
                   (<mget (mx-todos me)
                     (case (<mget (mx-find-matrix mx) :route) ;; <--- READ ROUTE PROPERTY
                       "All" :items
                       "Completed" :items-completed
                       "Active" :items-active
                       :items)))]
        (todo-list-item todo)))))
````
One popular CLJS routing library is [bide](https://github.com/funcool/bide). Bide knows nothing about Matrix dataflow, so we have a bit of glue to write to make the `:route` property dataflow-ready.
````clojure
(md/make ::md/todoApp
      .....
      :route (cI "All") ;; <--- cI makes :route an input cell, initialized to "All"
      :route-starter (r/start! (r/router [["/" :All]
                                          ["/active" :Active]
                                          ["/completed" :Completed]])
                       {:default     :ignore
                        :on-navigate (fn [route params query]
                                       (when-let [mtx @md/matrix]
                                         ;;; ... WRITE ROUTE PROPERTY
                                         (mset!> mtx :route (name route))))}))
````
Just two steps are required:
* wrap the `:route` property in an input Cell; and
* have the routing library `:on-navigate` handler write to the `:route` property.

The routing change then causes the list view to recompute which items to display, and an observer on the `UL` children arranges for the DOM to be updated.

#### git checkout todo-entry
Earlier we emphasized that mxWeb is an "un-framework". With this tag we add support for user entry of new to-dos, and illustrate one advantage of not being a framework: mxWeb does not hide the DOM.
````clojure
(defn todo-entry-field []
  (input {:class       "new-todo"
          :autofocus   true
          :placeholder "What needs doing?"
          :onkeypress  #(when (= (.-key %) "Enter")
                          (let [raw (form/getValue (.-target %))
                                title (str/trim raw)]
                            (when-not (str/blank? title)
                              (mswap!> (<mget @matrix :todos) :items-raw conj
                                (make-todo title)))
                            (form/setValue (.-target %) "")))}))
````
The token `%` of course is the raw DOM event. In a different handler we will see manipulation of the DOM classlist. Not every library allows easy access to the DOM, especially those such as ReactJS where we declaratively author VDOM. With ReactJS, accessing the DOM is a [heavier lift](https://reactjs.org/docs/refs-and-the-dom.html#callback-refs).

Speaking of raw DOM events, ReactJS hides those as well because ReactJS cannot handle their semantics. Example: `on-change` events fire on every keystroke on an input field. The MDN standard is that `on-change` fire only on blur or when the user hits enter, indicating they have completed their entry.

\<soapbox\>
ReactJS and every one of the [sixty-four submissions](http://todomvc.com/) to TodoMVC framework add a lot of value, but they also add their own baggage, and, like the Tower of Babel, segment the developer community, and limit library reuse. We need a front-end version of NoSQL.
\</soapbox\>
#### git checkout lifting-xhr
Before concluding, we look at an especially interesting example of lifting: XHR, affectionately known as Callback Hell. We do so exceeding the official TodoMVC spec to alert our user of any to-do item that returns results from a search of of the FDA [Adverse Events database](https://open.fda.gov/data/faers/).

Our treatment to date of [the XHR lift](https://github.com/kennytilton/matrix/tree/master/cljs/mxxhr) is technically minimal but the test suite includes clean dataflow solutions to several Hellish use cases. Our use case here is trivial, just a simple XHR query to the FDA API and one response, 200 indicating results found, 404 not. Notes follow the code.
````clojure
(defn adverse-event-checker [todo]
  (i
    {:class   "aes material-icons"
     :title "Click to see some AE counts"
     :onclick #(js/alert "Feature to display AEs not yet implemented")
     :style   (cF (str "font-size:36px"
                    ";display:" (case (<mget me :aes?)
                                  :no "none"
                                  "block")
                    ";color:" (case (<mget me :aes?)
                                :undecided "gray"
                                :yes "red"
                                ;; should not get here
                                "white")))}

    {:lookup   (cF+ [:obs (fn-obs (xhr-scavenge old))]
                 (make-xhr (pp/cl-format nil ae-by-brand
                             (js/encodeURIComponent
                               (de-whitespace (td-title todo))))
                   {:name       name :send? true
                    :fake-delay (+ 500 (rand-int 2000))}))
     :response (cF (when-let [xhr (<mget me :lookup)]
                     (xhr-response xhr)))
     :aes?     (cF (if-let [r (<mget me :response)]
                     (if (= 200 (:status r)) :yes :no)
                     :undecided))}
    "warning"))
````
    
That is the application code we can easily review in this project. To see where the response dataflow starts we must look at  mxXHR libary internals. (look for the `mset!>`; as for `with-cc`, we touch on that below):
````clojure
(defn xhr-send [xhr]
  (go
    (let [response (<! (client/get (<mget xhr :uri) {:with-credentials? false}))]
       (with-cc :xhr-handler-sets-responded
          (mset!> xhr :response
            {:status (:status response)
             :body   (if (:success response)
                       ((:body-parser @xhr) (:body response))
                       [(:error-code response)
                        (:error-text response)])}))))))
````
Hellish async XHR responses are now just ordinary Matrix inputs. 

Notes:
* `ae-checker-style-formula` manifests a nice code win: complex `cF`s can be broken out into their own functions;
* Google's Material Design icon fonts integrate smoothly;
* We fake variable response latency;
* For pedagogic reasons, we break up the lookup into several `cFs`:
* `lookup` functionally returns an mxXHR incarnation of an actual XHR, but...
* ...we specify that the XHR be *sent* immediately! This is where `with-cc` comes in...
* ...getting into the weeds, `with-cc` enqueues its body for execution at the right time in the datafow lifecycle...
* ...and should the user change the title and kick off a new lookup, an observer will GC the old one;
* `response` runs immediately, reads the nil lookup `response` but establishing the dependency, and returns nil;
* the `aes?` predicate runs immediately and does not see a `response`, so it returns `:undecided`;
* the color and display style properties decide on "gray" and "block";
* an mxWeb observer updates the DOM so we see a gray "warning" icon;
* when the actual XHR gets a response, good or bad, it is `<mset!` *with dataflow integrity* into the `response` property of the mxXHR;
* our AE checker `response` formula runs and captures the response;
* `aes?` runs, sees the response, and decides on :yes :or :no;
* the color and display style properties decide on new values;
* mxWeb does its thing and the warning disappears or turns red.

If you play with new to-dos, do *not* be alarmed by red warnings: all drugs have adverse events, and the FDA search is aggressive. You will also note an inefficiency we address in the next section, viz. that each to-do gets looked up anew each time the list changes. But first...

Matrix dataflow neutralizes the Hell of asynchronous callbacks because Matrix was created to propagate change gracefully. The `mset!>` of an asynchronously received response into the Matrix graph differs not at all from a user deciding to click their mouse or press a key. In either case, Matrix guarantees smooth, consistent propagation of the change throughout the graph of connected properties in accordance with what we call *datafow integrity*.

#### single source of behaviour
The XHR example is a great example of a quality we have barely touched on. When programming wuth mxWeb we benefit from having what we call a *single source of behavior* (SSB), with "source" as an unintended but welcome pun. The component `adverse-event-checker` is almost perfectly self-sufficient. It specifies the HTML, the semantics, the dynamic styling, and the XHR handling. It connects to the dataflow through the to-do `title` property and by generating style changes to reflect the FDA lookup response.

We find SSB a natural way to build applications. Authoring, debugging, and refactoring all go faster when related things are found together, or *co-located*, in the source

#### dataflow integrity
From the [Cells Manifesto](http://smuglispweeny.blogspot.com/2008/02/cells-manifesto.html), when application code assigns to some input cell X, the Cells engine guarantees:
* recomputation exactly once of all and only state affected by the change to X, directly or indirectly through some intermediate datapoint. Note that if A depends on B, and B depends on X, when B gets recalculated it may come up with the same value as before. In this case A is not considered to have been affected by the change to X and will not be recomputed;
* recomputations, when they read other datapoints, must see only values current with the new value of X. Example: if A depends on B and X, and B depends on X, when X changes and A reads B and X to compute a new value, B must return a value recomputed from the new value of X;
* similarly, client observer callbacks must see only values current with the new value of X; and...
* ...a corollary: should a client observer write to a datapoint Y, all the above must happen with values current with not just X, but also with the value of Y *prior* to the change to Y.
* deferred "client" code must see only values current with X and not any values current with some subsequent change to Y queued by an observer.

The astute reader will be surprised that observers, so carefully defined earlier *not* to be participants in the dataflow, are free to *initiate* dataflow. We make a careful distinction: they are allowed to do only as outsiders. The changes they initiate must be enqueued for execution *after* the change they are observing, as if they were event handlers initiating change.

As much fun as it was watching multiple async responses flow into the Matrix because we rebuilt all LIs to add or remove one, let us now fix that excess.
#### git checkout family-values
A matrix is a simple tree formed of single parents with multiple so-called `kids`, a nice short name for children. Normally we just list the kids, but when the list changes incrementally and the children are mxWeb widgets, the mxWeb observer will be rebuilding those hefty widgets and rebuilding the DOM on each small change.

To prevent this excess, Matrix has a small API we call "family values" after the [Charles Addams-inspired movie](https://www.youtube.com/watch?v=IHgfQ-0lYbg). The idea is to compute a collection of key values and then provide a factory function to be called with the key value to produce mxWeb instances only as needed after diffing the lists of key values.
````clojure
(defn todo-items-list []
  (section {:class "main"}
    (ul {:class "todo-list"}
      {:kid-values (cF (when-let [rte (mx-route me)]
                         (sort-by td-created
                           (<mget (mx-todos me)
                             (case rte
                               "All" :items
                               "Completed" :items-completed
                               "Active" :items-active)))))
       :kid-key #(<mget % :todo)
       :kid-factory (fn [me todo]
                      (todo-list-item todo))}
      ;; cache is prior value for this implicit 'kids' slot; k-v-k uses it for diffing
      (kid-values-kids me cache))))
````
Our key value is the abstract to-do model. `cache` above is a variable available to any property formula for the rare case when it wants to consider the prior computation when producing the next.

Now when you add or remove items, you will see AE lookups executed only for added items.

#### git checkout ez-dom
Above we promised more about having easy access to the DOM from front-end code, something one might take for granted but for the example of ReactJS where VDOM hides the DOM. We deliver on that promise with the last feature we will implement: the ability to edit a to-do after it has been entered. 

First, we drop a new input field into each LI dedicated to a to-do item:
````clojure
(input {:class     "edit"
            :onblur    #(todo-edit % todo true)
            :onkeydown #(todo-edit % todo (= (.-key %) "Enter"))})
````
And now the handler, where DOM access is substantial:
````clojure
(defn todo-edit [e todo edit-commited?]
  (let [edt-dom (.-target e)
        li-dom (dom/getAncestorByTagNameAndClass edt-dom "li")]

    (when (classlist/contains li-dom "editing")
      (let [title (str/trim (form/getValue edt-dom))
            stop-editing #(classlist/remove li-dom "editing")]
        (cond
          edit-commited?
          (do
            (stop-editing)                                  ;; has to go first cuz a blur event will sneak in
            (if (= title "")
              (td-delete! todo)
              (mset!> todo :title title)))

          (= (.-key e) "Escape")
          ;; this could leave the input field with mid-edit garbage, but
          ;; that gets initialized correctly when starting editing
          (stop-editing))))))
````
While the above seems like there should be a better way, we see the same code in many TodoMVC solutions, probably for the reason documented in a comment above: an extraneous blur event when halting editing. We assure these are ignored by directly altering the DOM classlist instead of going through the mxWeb/Matrix lifecycle which would remove the "editing" class too late.

Finally, we have dropped in one last feature from the TodoMVC spec that makes little U/X sense but does let us demonstrate how me hacked around some unhelpful browser behavior, viz., overriding our own manipulation of a checkbox's checked status.
````clojure
(defn toggle-all []
  (div {} {;; 'action' is an ad hoc bit of intermediate state that will be used to decide the
           ;; input HTML checked attribute and will also guide the label onclick handler.
           :action (cF (if (every? td-completed (mx-todo-items me))
                         :uncomplete :complete))}

    (input {:id        "toggle-all"
            :class     "toggle-all"
            ::mxweb/type "checkbox"
            :checked   (cF (= (<mget (mx-par me) :action) :uncomplete))})

    (label {:for     "toggle-all"
            :onclick #(let [action (<mget me :action)]

                        ;; else browser messes with checked, which we handle
                        (event/preventDefault %)

                        (doseq [td (mx-todo-items)]
                          (mset!> td :completed (when (= action :complete) (util/now)))))}
      "Mark all as complete")))
````
#### The missing TodoMVC requirement
Nothing will be added by implementing persistence, but if you are curious you can check out [mxLocalStorag](https://github.com/kennytilton/matrix/blob/master/js/matrix/js/Matrix/mxWeb.js) at the very end of the source from our Javascript implementation of mxWeb. 

## Summary

## License: MIT

Copyright © 2018 Kenneth Tilton

Distributed under the MIT License.
