# Building TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*, in depth

> Have you read [the preamble](../README.md) to this write-up? If so, you might want to skip down to "Set Up". If not, you might want to start there for a gentler introduction to Matrix and mxWeb.

> In our final episode we will dive quite deep into Matrix and mxWeb, but just deep enough to see past the syntactic sugar. Some of what follows reprises material offered so far, just to have a coherent narrative thread.

We choose *mxWeb* as the vehicle for introducing Matrix because nothing challenges a developer more than keeping application state straight while an intelligent user does their best to use a rich interface correctly. Then marketing wants a U/X overhaul.

> "UIs...I am not going to go there. I don't do that part."  
-- Rich Hickey on the high ratio of code to logic in UIs, *Clojure/Conj 2017*

mxWeb is a thin web un-framework built atop Matrix. We say "un-framework" because mxWeb exists only to wire the DOM for dataflow. The API design imperative is that the MDN reference be the mxWeb reference; mxWeb itself introduces no new architecture.

Matrix achieves this simply by enhancing how we initialize, read, and write individual properties:
* properties can be initialized as a literal value or as a function;
* should some property `A` be initialized with a literal, we can write to it;
* should a functional property `B` read `A`, `A` remembers `B`;
* when we write to `A`, `A` tells `B`; and
* we can supply "on change" callbacks for properties.

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

## Excerpts from "Building TodoMVC", in depth
As in our prior run through, we will work through the evolving TodoMVC by checking out different tags.

### enter-todos
We can now start our demo matrix off with a few preset to-dos. Some things to note:
* the optional first "type" parameter ::todoApp is supplied; we will use that in various Matrix searches to identify the root of the TodoMVC matrix.
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


