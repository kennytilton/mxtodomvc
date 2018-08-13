# Building TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*, continued

> Have you read [the preamble](../README.md) to this write-up? If not, you might want to start there for a gentler introduction to Matrix and mxWeb.

Let's get "hello, Matrix" running and then start building [TodoMVC](http://todomvc.com) from scratch. 

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
When starting on a TodoMVC implementation, we first execute just the title and footer as our own little "hello, world". Let us jump now to the commit of that milestone:
````bash
git checkout hello-todomx
````
From now on, our cue to check out a new tag will be these headers:
#### git checkout hello-todomx
And here is the mxWeb HTML work-aike, look-alike code for "hello, todomx":
````clojure
(defn matrix-build []
  (md/make
    :mx-dom (section {:class "todoapp"}
              (header {:class "header"}
                (h1 "todos")
                (mxtodo-credits)))))
````
The "tags" such as `section`, `header`, and `h1` are CLJS macros. mxWeb is *all* CLJS.

As with HTML, each mxWeb tag macro takes the same parameters:
* an optional map of DOM attributes;
* unique to mxWeb, an optional map of custom application properties; and
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
Hello, [Web Components](https://developer.mozilla.org/en-US/docs/Web/Web_Components). `mxtodo-credits` is rather simple as components go, but next up is one that takes parameters to support reuse.

### git checkout wall-clock
Reminder:
````bash
git checkout wall-clock
````
The TodoMVC spec does not include a time or date display. We added a simple "wall clock" because it lets us take a quick, deep dive into mxWeb in just a few lines of code. Here is what we will see:
* automatic state management: our first dataflow;
* transparent state management;
* DOM efficiency without VDOM complexity;
* the mxWeb version of Web Components;
* all dataflow all the time: "lifting" components into the Matrix;
* a single source of behavior: co-location of model and view; and
* the Grail of object reuse.

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
Now let's work through the bullets above and see how they are manifested in the code above.

#### automatic, transparent state management
On every interval, we feed the browser clock epoch into the application Matrix `clock` property. The child string content of the DIV gets regenerated because `clock` changed. There is no explicit publish or subscribe. We simply read with `mget` and assign with `mset!>`.

#### DOM efficiency without VDOM
Property-to-property dataflow means the system knows with fine granularity when and what DOM needs updating when new inputs hit the Matrix. 
#### the mxWeb approach to Web Components
The function `wall-clock` has four parameters, `[mode interval start end]`. Achieving component reuse with mxWeb differs not at all from parameterizing any Clojure function for maximum utility.
#### all dataflow all the time: "lifting" components into the Matrix  
We want to program with dataflow as much as possible, but browsers do not know about Matrix dataflow. If the component will not come to the Matrix, the Matrix will wrap the component: we write more or less "glue" code to bring it into the datafow.  
````clojure
(js/setInterval
    #(mset!> me :clock (util/now))
    interval)
````  
We call such glue "lifting". Lifting the system clock required just a few lines of code. We hinted earlier that mxWeb exemplifies "lifting". That took six hundred lines.
#### a single source of behavior: co-location of model and view  
Our wall clock widget needs application state, and it generates and relays that state itself. The `clock` property holds the JS epoch, and the 'ticker' property holds a timer driving `clock`. Nearby in the code, a child element consumes the stream of `clock` values. Everything resides together in the source for quick authoring, debugging, revision, and understanding.
  
#### the Grail of object reuse  
Few DIV elements need a stream of clock values, so normally we would need to sub-class DIV to arrange for one. Matrix, like the prototype model of OOP, lets us code up a new dataflow-capable clock property on the fly and attach it to our proxy `div`.

### git checkout enter-todos
As promised, that was a deep first dive. This tag will be simpler, adding a bunch more UI structure but no ability to edit or even create todos:
* we load a few fixed-todos at start-up;
* we show them in a list;
* one control lets us toggle a to-do between completed or not; and
* another control logically deletes a to-do.

Here is the code to make a to-do. `cI` is shorthand for making an "input cell", one to which we can assign new values from imperative code in an event handler.
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
Apparent booleans like `:completed` will in fact be nil or timestamps, so no "?" suffixes.

Here is the to-do-list container model, set up to take a list of hard-coded initial to-dos to get us rolling:
````clojure
(defn todo-list [seed-todos]
  (md/make ::todo-list
    :items-raw (cFn (for [to-do seed-todos]
                      (make-todo to-do})))
    :items (cF (doall (remove td-deleted (<mget me :items-raw))))))
````
The bulk of the to-do app does not care about deleted to-dos, so we use a clumsy name "items-raw" for the true list of items ever created, and save the name "items" for the ones actually used. `cFn` is short for "formulaic then input", meaning the property is initialized by running the formula and thereafter is set by imperative code.

We can now start our demo matrix off with a few preset to-dos. Some things to note:
* the optional first "type" parameter ::todoApp is supplied
* building the matrix DOM is now wrapped in `(cFonce (with-par me ...)`;
  * `cFonce` effectively defers the enclosed form until the right lifecycle point in the matrix's initial construction.
  * `with-par me` is how the matrix DOM knows where it is in the matrix tree. All matrix nodes know their parents so they can navigate the tree freely to gather information.
* the app credits are now provided by a new "web component", and that along with the "wall-clock" reusable are off in their own namespace.
* most interesting is `(mxu-find-type me ::todoApp)`, a bit of exposed wiring that demonstrates how Matrix elements pull information from elsewhere in the Matrix using various "mx-find-\*" CSS selector-like utilities. 

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

We can now play with toggling the completion state of to-dos, deleting them directly, or deleting them with the "clear completed" button, keeping an eye on "items remaining".  

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
The token `%` is the raw DOM event. In a different handler we will see manipulation of the DOM classlist. 

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
