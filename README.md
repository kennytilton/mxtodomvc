# TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*

The *Matrix* dataflow library endows application state with causal power, freeing us of the burden of propagating state change across highly interdependent models. More grandly, it brings our application models to life, animating them in response to streams of external inputs.

Matrix does this simply by enhancing how we initialize, read, and write individual properties:
* properties can be initialized as a literal value or as a function;
* should some property `A` be initialized with a literal, we can write to it;
* should a functional `B` read `A`, `A` remembers `B`;
* when we write to `A`, `A` tells `B`; and
* we can supply "on change" callbacks for properties.

What does it mean for one property to read another, for `B` to read `A`? It means declaring `B` as an arbitrary HLL function of `A` and possibly others. In pseudo code:
````clojure
B <= (fn [] (+ 42 A C))))
````
What does it mean for `A to tell `B`? `A` makes `B` compute a new value, *causing* it to change. 

What happens when `B` computes a new value? `B` might have its own dependent properties to tell. `A` or `B` might also want to act on the world outside the graph of properties. 
> "Nothing messes with functional purity quite like the need for side effects. On the other hand, effects are marvelous because they move the app forward." - [re-frame intro](https://github.com/Day8/re-frame)

A Web game app may use a CLJS map to model a Romulan warship and a paired DOM element to render it. If `A` is the `cloaked` property of the map warship and it changes, the "hidden" attribute of the DOM warship needs to be added or removed. To this end, Matrix lets us define "on-change" *observers*.

> [observer](https://dictionary.cambridge.org/dictionary/english/observer): noun. UK: /əbˈzɜː.vər/, US: /əbˈzɝː.vɚ/  A person who watches what happens but has no active part in it.

Observers are *monitors* of the dataflow between a graph of properties, not participants in that flow. They act, but they act outside the dataflow graph.

#### lifting
What about X, Y, and Z? i.e., Properties from existing libraries that know nothing about Matrix? We write whatever "glue" code it takes to wire existing libraries with dataflow. We call this "lifting" libraries into the Matrix. Lifting the DOM into mxWeb required about two thousand lines of code. We will see several examples below of lifting. 

#### matrix?
`A` might not be a simple, descriptive property such as "cloaked". `A` might be `K` for "kids" and hold the child nodes of some parent; the very population of our application model can shrink or grow with events. We call this dynamic population of communicating nodes a *matrix*.

> ma·trix ˈmātriks *noun* an environment in which something else takes form. *Origin:* Latin, female animal used for breeding, parent plant, from *matr-*, *mater*

Simply by propagating change between functional properties, with strictly segregated dataflow to and from the outside world, the Matrix library brings applications to life.

#### Really?
Can we really program this way? This [Algebra](https://tiltonsalgebra.com/#) app consists of about twelve hundred `A`s and `B`s, and extends into a Postgres database. Everything runs under matrix control. It lifts Qooxdoo JS, MathJax, Postgres and more. The average number of dependencies for one value is a little more than one, and the deepest dependency chain is about a dozen. On complex dispays of many math problems, a little over a thousand values are dependent on other values.

#### Related work
> "Derived Values, Flowing" -- the [re-frame](https://github.com/Day8/re-frame/blob/master/README.md) tag-line

Matrix enjoys much good company in this field. We believe Matrix offers more simplicity, transparency, granularity, expressiveness, efficiency, and functional coverage, but in each dimension differs only in degree, not spirit. Other recommended CLJS libraries are [Reagent](https://reagent-project.github.io/), [Hoplon/Javelin](https://github.com/hoplon/javelin), and [re-frame](https://github.com/Day8/re-frame). Beyond CLJS, we admire [MobX](https://github.com/mobxjs/mobx/blob/master/README.md) (JS), [binding.Scala](https://github.com/ThoughtWorksInc/Binding.scala/blob/11.0.x/README.md), and Python [Trellis](https://pypi.org/project/Trellis/). Let us know about any we missed.

#### mxWeb, "poster" application
*mxWeb* is a thin web un-framework built atop Matrix. We introduce Matrix with mxWeb because nothing challenges a developer more than keeping application state straight while an intelligent user does their best to use a rich interface correctly. Then marketing wants a U/X overhaul.

We say "un-framework" because mxWeb exists only to wire the DOM for dataflow. The API design imperative is that the MDN reference be the mxWeb reference; mxWeb itself introduces no new architecture.

#### TodoMVC
So far, so abstract. Ourselves, we think better in concrete. Let's get "hello, Matrix" running and then start building TodoMVC from scratch. 

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
Above we see the potential for custom HTML tags wrapping arbitrarily complex, reusable native DOM clusters, aka [Web Components](https://developer.mozilla.org/en-US/docs/Web/Web_Components). `mxtodo-credits` is rather simple, but next up is a function/component taking four parameters to support reuse.

Note also that, yes, we can mix standard CLJS with our "HTML" because, again, it is all CLJS.
### git checkout wall-clock
Reminder!
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
#### lifting-xhr
Before concluding, we look at an especially interesting example of lifting: XHR, affectionately known as Callback Hell. We do so exceeding the official TodoMVC spec to alert our user of any to-do item that returns results from a search of of the FDA [Adverse Events database](https://open.fda.gov/data/faers/).

Our treatment to date of [the XHR lift](https://github.com/kennytilton/matrix/tree/master/cljs/mxxhr) is technically minimal but the test suite includes clean dataflow solutions to several Hellish use cases. Our use case here is trivial, just a simple XHR query to the FDA API and one response, 200 indicating results found, 404 not. Notes follow the code.
````clojure
(defn ae-checker-style-formula []
  (cF (str "font-size:36px"
  
        ";display:"
        (if (<mget me :lookup) "block" "none")

        ";color:"
        (cond
          (<mget me :aes?) "red"
          (<mget me :loookup) "gray"
          :default "green"))))
          
(defn adverse-event-checker [todo]
  (i {:class "aes material-icons"
      :style (ae-checker-style-formula)}

    {:lookup   (cF+ [:obs (fn-obs (xhr-scavenge old))]
                 (make-xhr (pp/cl-format nil ae-by-brand
                             (js/encodeURIComponent (td-title todo)))
                   {:name name :send? true
                    :fake-delay (+ 500 (rand-int 2000))}))
     :response (cF (when-let [lookup (<mget me :lookup)]
                     (<mget lookup :response)))
     :aes?      (cF (if-let [r (<mget me :response)]
                     (= 200 (:status r))))}
    "warning"))````
That is the application code. The mxXHR libary internals show the datalow integration. (look for the `mset!>`; `with-cc` we touch on below):
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
Notes:
* `ae-checker-style-formula` manifests a nice code win: complex `cF`s can be broken out into their own functions;
* Google's Material Design icon fonts integrate smoothly;
* We fake variable response latency;
* For pedagogic reasons, we break up the lookup into several `cFs`:
* `lookup` funtionally returns an mxXHR incarnation of an actual XHR, but...
* ...we specify that the XHR be *sent* immediately! This is where `with-cc` comes in...
* ...getting into the weeds, `with-cc` enqueues its body for execution at the right time in the datafow lifecycle;
* `response` runs immediately, sees the lookup and returns nil;
* the `aes?` predicate runs immediately and does not see a `response`, so it returns `:undecided`;
* when the actual XHR gets a response, good or bad, it is `<mset!` *with dataflow integrity* into the `response` property of the mxXHR;
* our AE checker `response` formula runs and captures the response;
* `aes?` runs, sees the response, and decides on :yes :or :no;
* the color and display style properties decide on new values.


## License: MIT

Copyright © 2018 Kenneth Tilton

Distributed under the MIT License.
