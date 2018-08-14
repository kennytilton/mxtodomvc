# Building TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb, continued*

The [TodoMVC](http://todomvc.com) project specifies a trivial Web application as the basis for comparing Web frameworks. In [the preamble](../README.md) to this write-up, we looked at what makes programming with Matrix and mxWeb different, only lightly touching on mxTodoMVC code. 

> If you have not read that preamble, you might want to start there for a gentler introduction to Matrix and mxWeb.

In this write-up, we will checkout a series of git tags marking milestones along our incremental evolution of TodoMVC. At each stage we will review the new code, but not delve too deeply into the mxWeb mechanics. That we will save for a final, [detailed exploration](InDepth.md) for those of us who need to see all the moving parts to be comfortable with high level APIs, especially those that seem like magic.

Let's get "hello, Matrix" running and start building TodoMVC from scratch. 

## Set-Up

````bash
git clone https://github.com/kennytilton/mxtodomvc.git
cd mxtodomvc
lein deps
lein clean
lein fig:build
````
This will auto compile and send all changes to the browser without the need to reload. After the compilation process is complete, you will get a Browser Connected REPL. A web page should appear on a browser near you with a header saying just "hello, Matrix".

<img height="48px" align="center" src="pix/hello-matrix.png?raw=true">

For issues, questions, or comments, ping us at kentilton on gmail, or DM @hiskennyness on Slack in the #Clojurians channel.

## Building TodoMVC from Scratch
When starting on a TodoMVC implementation, we first execute just the title and footer as our own little "hello, world". Let us jump now to the commit of that milestone:
````bash
# Control-D
git checkout hello-todomx
lein fig:build
````
Things should now look more Todo-ish:

<img height="144px" align="center" src="pix/hello-todomx.png?raw=true">

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

Just to complete the picture, and without further discussion, the above gets installed in our Web page thus:
````clojure
(let [root (dom/getElement "tagroot")
      app-matrix (matrix-build)]
  (set! (.-innerHTML root) nil)
  (dom/appendChild root
    (tag-dom-create
      (<mget app-matrix :mx-dom))))
````
Back to the TodoMVC code. As with HTML, each mxWeb tag macro takes the same parameters:
* an optional map of DOM attributes;
* unique to mxWeb, and omitted here, an optional map of custom application properties; and
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

### wall-clock
We add a simple "wall clock". It is not in the TodoMVC spec, but it lets us take a quick, deep dive into mxWeb in just a few lines of code. Here is what we will see:
* automatic, transparent state management: our first dataflow;
* DOM efficiency without VDOM;
* the mxWeb version of Web Components;
* all dataflow all the time: "lifting" components into the Matrix;
* a single source of behavior (SSB): co-location of model and view; and
* the Grail of object reuse.
````bash
# Control-D
git checkout wall-clock
lein fig:build
````
<img height="192px" align="center" src="pix/wall-clock.png?raw=true">

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
The first `wall-clock` shows the date and updates every hour, the second shows the time second by second. And now the component:
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
Now let's work through the bullets and see how they are manifested in the code above.

#### automatic, transparent state management
On every interval, we feed the browser clock epoch into the application Matrix `clock` property. The child string content of the DIV gets regenerated because `clock` changed. There is no explicit publish or subscribe; we simply read with `<mget` and assign with `mset!>`.
#### DOM efficiency without VDOM
As the UI clock ticks, the only DOM update made is to set the innerHTML of the `div`. Property-to-property dataflow tells us with fine granularity exactly what DOM changes must be made.
#### the mxWeb approach to Web Components
The function `wall-clock` has four parameters, `[mode interval start end]`. mxWeb "components" are as flexible as any Clojure function we care to define.
#### all dataflow all the time: "lifting" components into the Matrix  
Browsers do not know about the Matrix, so we write more or less "glue" code to bring the system clock into the dataflow.  
````clojure
(js/setInterval
    #(mset!> me :clock (util/now))
    interval)
````  
We call such glue "lifting". Lifting the system clock required just a few lines of code. We hinted earlier that mxWeb exemplifies lifting. It required six hundred lines.
#### a single source of behavior (SSB): co-location of model and view  
Our wall clock widget needs application state, and it generates and relays that state itself. The `clock` property holds the JS epoch, and the 'ticker' property holds a timer driving `clock`. Nearby in the code, a child element consumes the stream of `clock` values. Everything resides together in the source for quick authoring, debugging, revision, and understanding. We call this having a *single source of behavior*, or SSB.
#### the Grail of object reuse  
Few DIV elements need a stream of clock values. In a rigid OOP framework, we would need to sub-class DIV to arrange for one. Matrix, like the prototype model of OOP, lets us code up a new dataflow-capable clock property on the fly and attach it to our proxy `div`.

### enter-todos
As promised, that was a deep first dive. This tag will be simpler, adding a bunch more UI structure but no ability to edit or even create todos:
* we load a few fixed-todos at start-up;
* we show them in a list;
* one control lets us toggle a to-do between completed or not; and
* another control logically deletes a to-do.
````bash
# Control-D
git checkout enter-todos
lein fig:build
````
<img height="384px" align="center" src="pix/enter-todos.png?raw=true">

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

We can now start our demo matrix off with a few preset to-dos. 

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
The TodoMVC credits are now delivered by a new *app-credits* component. More interesting is `(mxu-find-type me ::todoApp)`, a bit of exposed wiring demonstrating how Matrix elements pull information from elsewhere in the Matrix; they navigate to Matrix nodes with the information they need using various "mx-find-\*" CSS selector-like utilities.

And now the to-do item view itself.
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
  (mswap!> td :completed
    #(when-not % (util/now))))
````
We execute a simple dashboard as well, without the filters just yet:
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
`mx-todos` and `mx-todo-items` wrap the complexity of navigating the Matrix to find that desired data.

We can now play with toggling the completion state of to-dos, deleting them directly, or deleting them with the "clear completed" button, keeping an eye on "items remaining".  

Next up: the spec requires a bit of routing.

#### lift-routing
The official TodoMVC spec requires a routing mechanism be used to implement the user filtering of which to-dos are displayed, based on their completion status. The options are "all", "completed" only, and "active" (incomplete) only.
````bash
# Control-D
git checkout lift-routing
lein fig:build
````
<img height="384px" align="center" src="pix/lift-routing.png?raw=true">

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

#### todo-entry
Earlier we emphasized that mxWeb is an "un-framework". With this tag we add support for user entry of new to-dos, and illustrate one advantage of not being a framework: mxWeb does not hide the DOM. Our next feature -- allowing the user to enter the to-dos -- benefits a couple of places from direct DOM and event access.
````bash
# Control-D
git checkout todo-entry
lein fig:build
````
<img height="384px" align="center" src="pix/todo-entry.png?raw=true">

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
mxWeb callbacks are passed the raw browser event, above accessed as the token `%`. `getValue` is straight JS, and with `setValue` we easily implement the spec requirement to clear the field. In a different handler we will see manipulation of the DOM classlist. No [`refs` heavy lift](https://reactjs.org/docs/refs-and-the-dom.html) required.

#### lifting-xhr
Now an especially interesting example of lifting: XHR, affectionately known as Callback Hell. We do so exceeding the official TodoMVC spec to alert our user of any to-do item that returns results from a search of the FDA [Adverse Events database](https://open.fda.gov/data/faers/).
````bash
# Control-D
git checkout lifting-xhr
lein fig:build
````
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
                   {:name       name
                    :send? true
                    :fake-delay (+ 500 (rand-int 2000))}))
     :response (cF (when-let [xhr (<mget me :lookup)]
                     (xhr-response xhr)))
     :aes?     (cF (if-let [r (<mget me :response)]
                     (if (= 200 (:status r)) :yes :no)
                     :undecided))}
    "warning"))
````
    
That is the application code. To see where the response dataflow starts we must look at mxXHR libary internals. (Look for the `mset!>`; as for `with-cc`, we touch on that below):
````clojure
(defn xhr-send [xhr]
  (go
    (let [response (<! (client/get (<mget xhr :uri) {:with-credentials? false}))]
       (with-cc :xhr-handler-sets-responded
          (mset!> xhr :response    ;; <------- DATAFLOW BEGINS HERE
            {:status (:status response)
             :body   (if (:success response)
                       ((:body-parser @xhr) (:body response))
                       [(:error-code response)
                        (:error-text response)])}))))))
````
Hellish async XHR responses are now just ordinary Matrix inputs. 

Notes:
* We fake variable response latency;
* `with-cc`, an advanced trick, enqueues its body for execution at the right time in the datafow lifecycle.

> If you play with new to-dos, do *not* be alarmed by red warnings: all drugs have adverse events, and the FDA search is aggressive: cats have adverse events. Dogs are fine.

You will also note an inefficiency to be addressed in the next section: every to-do gets looked up anew each time the list changes. Let us fix that.
#### family-values
A matrix is a simple tree formed of single parents with multiple so-called `kids`, a nice short name for children. When the list changes incrementally and the children are mxWeb widgets, the mxWeb observer will be rebuilding those hefty widgets on each small change.
````bash
# Control-D
git checkout family-values
lein fig:build
````
To prevent this excess, Matrix has a small API we call "family values" after the [Charles Addams-inspired movie](https://www.youtube.com/watch?v=IHgfQ-0lYbg). The idea is to compute a collection of key values and provide a factory function to be called with the key value to produce mxWeb instances only as needed after diffing the key values.
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
Our key is the abstract to-do model. `cache` above is a variable supplied by the `cF` macro. It will be bound to the prior computation for a formula, or the symbol `unbound` on the first invocation. `kid-values-kids` does the work of diffing new and prior values and calling the factory as needed for new values.

We do not offer a diagram here because this you have to see live: when you add an item, you will see an AE lookup executed only for the new item. When you delete an item, no lookups will be executed. Before this version, all to-dos were looked up on each change.

#### ez-dom: editing an existing to-do
Above we promised more about having easy access to the DOM from front-end code, something one might take for granted but for the example of ReactJS where the `refs` rigmarole is required. We deliver on that promise with the last feature we will implement: the ability to edit a to-do after it has been entered. 
````bash
# Control-D
git checkout family-values
lein fig:build
````
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
While the above seems like there should be a better way, we see the same code in many TodoMVC solutions, probably for the reason documented in a comment above: browsers deliver an extraneous blur event when halting editing. We ensure these are ignored by directly altering the DOM classlist instead of going through the mxWeb/Matrix lifecycle, which removes the "editing" class too late to head off the extra blur.

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
#### The missing TodoMVC requirement: lifting local-storage
Nothing will be added by implementing persistence, but if you are curious you can check out [mxLocalStorag](https://github.com/kennytilton/matrix/blob/master/js/matrix/js/Matrix/mxWeb.js) at the very end of the source from our Javascript implementation of mxWeb. 

## Summary
That completes our implementation of the TodoMVC spec. In our next in-depth write-up of mxWeb, we will look more closely at certain elements to fully lift any sense of mystery created by the dataflow paradigm.
