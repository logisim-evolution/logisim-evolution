logisim-evolution
=================
[![Codewake](https://www.codewake.com/badges/codewake.svg)](https://www.codewake.com/p/logisim-evolution)
[![Codewake](https://www.codewake.com/badges/codewake2.svg)](https://www.codewake.com/p/logisim-evolution)

Logisim is an educational tool for designing and simulating digital logic circuits.
It has been originally created by [Dr. Carl Burch](http://www.cburch.com/logisim/) and actively developed until 2011.
After this date the author focused on other projects, and recently the development has been officially stopped  [(see his message here)](http://www.cburch.com/logisim/retire-note.html).

In the meantime, people from a group of swiss institutes ([Haute École Spécialisée Bernoise](http://www.bfh.ch), [Haute École du paysage, d'ingénierie et d'architecture de Genève](http://hepia.hesge.ch), and [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch)) started developing a version of Logisim that fitted their courses, integrating several tools -- for instance a chronogram, the possibility to test the schematics directly on an electronic board, TCL/TK consoles, ...

The project is currently maintained by the [REDS Institute](http://reds.heig-vd.ch), which is part of the [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch), Yverdon-les-Bains, Switzerland.

We have decided to release this new Logisim version under the name logisim-evolution, to highlight the large number of changes that occurred in these years, and **we actively seek the contribution of the community**.

## What's new in logisim-evolution
* chronogram -- to see the evolution of signals in your circuit
* electronic board integration -- schematics can now be simulated on real hardware!
* board editor -- to add new electronic boards
* VHDL component -- a new component type whose behavior is specified in VHDL
* TCL/TK console -- interfaces between the circuit and the user
* DIP switches
* RGB LEDs
* large number of bug-fixes
* GUI improvements
* automatic updates
* code refactoring
* ...

## How to install logisim-evolution
You can find an already compiled **stable** version of the code [here](http://reds-data.heig-vd.ch/logisim-evolution/logisim-evolution.jar).
To execute it, click on the downloaded file or type in a console
```bash
java -jar logisim-evolution.jar
```

You can also compile it by yourself by cloning the repository on your local machine. Once this is done, enter the directory and execute
```bash
ant run
```
This also creates locally a .jar file, that you can distribute and use on other machines.

## Documentation
[Here](http://reds-data.heig-vd.ch/logisim-evolution/IntroToLogisimEnglish.pdf)  you can find a tutorial (French version [here](http://reds-data.heig-vd.ch/logisim-evolution/tutoLogisim.pdf)) that explains some basic usage of Logisim. The electronic card referenced in the tutorial is a small card we use in our laboratories -- you won't be able to buy it in a store -- but the descriptions should be good enough to be used for another generic board.

Another good reference is [this book](http://www.lulu.com/shop/george-self/exploring-digital-logic-with-logisim-ebook/ebook/product-21118223.html), where basic electronics is explained with the help of Logisim.

## Editing logisim-evolution in Eclipse
To import directly logisim-evolution in Eclipse, you can use Eclipse's import wizard:

*Import -> git project -> [put the connection details] -> New project -> Java project from Ant*

You will, however, encounter a problem when you will try to execute the code. In particular, an exception *ExceptionInInitializerError* will be thrown. To solve this, execute the *eclipse_fix.sh* script in the program's directory, or go in the *bin/* subdirectory and create links to the following directories available in the program's directory
* *boards_model*
* *javax*
* *libs*
* *resources*
* *doc*

## Retro-compatibility
We cannot assure retro-compatibility of logisim-evolution with files created with the original Logisim.
We have incorporated a parser that alters the name of the components to satisfy VHDL requirements for variable names,
but components evolved in shape since then (think, for instance, to RAM and counters).
You might need to rework a bit your circuits when opening them with logisim-evolution -- but the changes will be stored
in the new format, therefore you have to do your work only once.

## Wish-list
Logisim-evolution is a continuously-growing software, and we have several ideas we would like to implement. In particular, we would like to have
* unit tests for the code
* extensive documentation
* test circuits
* ...
If you are willing to contribute with any of these, please feel free to contact us!

## How to get support for logisim-evolution
Unfortunately, we do not have enough resources to provide direct support for logisim-evolution.
We will, however, try to deal with the raised issues in a *best-effort* way.

If you find a bug or have an idea for an interesting feature, please do not hesitate to open a ticket!

## License
The code is licensed under the GNU GENERAL PUBLIC LICENSE, version 3.

## Credits
The following institutions/people actively contributed to Logisim-evolution:
* Carl Burch - Hendrix College - USA
* [Haute École Spécialisée Bernoise](http://www.bfh.ch) - Switzerland
* [Haute École du paysage, d'ingénierie et d'architecture de Genève](http://hepia.hesge.ch) - Switzerland
* [Haute École d'Ingénierie et de Gestion du Canton de Vaud](http://www.heig-vd.ch) - Switzerland
* Theldo Cruz Franqueira - Pontifícia Universidade Católica de Minas Gerais - Brasil
* Moshe Berman - Brooklyn College

If you feel that your name should be in this list, please feel free to send us a [mail](mailto:roberto.rigamonti@heig-vd.ch)!

## Other Logisim forks available on the net
* Logisim by Joseph Lawrance et al. [(link)](https://github.com/lawrancej/logisim) - they have started from Burch's original code and integrated it in several open-source development frameworks, cleaning up the code. We have taken a few code cleanups and the redo functionality from their code.
* logisim-iitd [(link)](https://code.google.com/p/logisim-iitd) - IIT Delhi version of Logisim, it integrates the Floating-Point Components within the Arithmetic Unit.
* Logisim for the CS3410 course, Cornell's University [(link)](http://www.cs.cornell.edu/courses/cs3410/2015sp/) - they have a very interesting test vector feature, that has only recently integrated in logisim-evolution.

## Alternatives
* A complete rewriting of Logisim, called Digital, has been developed by Prof. Helmut Neemann of the Baden-Württemberg Cooperative State University Mosbach. You can find it [(here)](https://github.com/hneemann/Digital).
