# mxTodoMVC
#### An Introduction by Example to Matrix dataflow and mxWeb

The `Matrix` dataflow library endows application state with causal power over other such state, freeing the developer from the burden of reliably propagating unpredictable change across highly interdependent models. It does this by intervening at the fundamental level of reading and writing properties, and by providing a precise mechanism by which application models can operate on the world, if only to update a computer screen.

More grandly, Matrix brings our application models to life, animating them in response to streams of external inputs. Hence the name.

> ma·trix ˈmātriks *noun* an environment in which something else takes form. *Origin:* Latin, female animal used for breeding, parent plant, from *matr-*, *mater*

The movies were fun, but Mr. Hickey might disapprove the misconstruction of the name.

### You say "reactive", we say "dataflow"
Most folks today call this _reactive programming_. That describes well the programmer mindset in the small, but we find _dataflow_ more descriptive of the emergent systems. A financial analyst builds a spreadsheet model of their enterprise by writing individual cell formulas, yes, but their goal is to play "what if?" by changing critical inputs and watching the effect sweep across the model.

Matrix enjoys much good company in this field. We believe Matrix offers more simplicity, transparency, granularity, expressiveness, efficiency, and functional coverage, but in each dimension differs in degree, not spirit. Other recommended CLJS libraries are Reagent, Hoplon/Javelin, and re-frame. Beyond CLJS, we admire MobX (JS), binding.Scala, and Python Trellis.

### mxWeb, "poster" application
`mxWeb` is a thin web un-framework built atop Matrix. We introduce Matrix in the context of mxWeb, because nothing challenges a developer more than keeping application state consistent while an intelligent user does their best to use a rich interface correctly. Then marketing wants the U/X redone.

We say "un-framework" because mxWeb  exists only to equip the DOM for dataflow. The API design imperative is that the MDN reference be the mxWeb reference; mxWeb itself introduces no new architecture.

### TodoMVC
So far, so abstract. Ourselves, we think better in concrete. Let's get "hello, Matrix" running and then start building TodoMVC from scratch. 

The TodoMVC project specifies a trivial Web application to be the basis for comparing Web frameworks. We will first satisfy the requirements, then extend the spec to include XHRs. Along the way we will tag milestones so the reader can visit any stage of development.

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

## Building mxTodoMVC from Scratch
RSN

## License

Copyright © 2018 Kenneth Tilton

Distributed under the MIT License.