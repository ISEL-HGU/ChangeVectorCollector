# ChangeVectorCollector

Uses the BIC information collected from DPminer to embed the BICs into a vector.
The vector embedding is done by defining the type of changed nodes and their AST using an algorithm called Gumtree.

## Order of Scenario:
1. making buggy train set
    a. description: use BIC collected from DPMiner to vectorize commit by applying the Gumtree algorithm.
    b. run ./make_buggy_train.sh located in the parent folder of 3 projects.
2. making clean train set
    a. description: use clean commits to vectorize commit by applying the Gumtree algorithm.
    b. run ./make_clean_train.sh located in the parent folder of 3 projects.
3. combine clean and buggy train set for a combined SimFin model.
4. making test set
    a. description: make test data by collecting all clean and buggy commits and also vectorize them using Gumtree.
    b. run ./make_test located in the parent folder of 3 projects.

<br>

## How to build: Gradle
<pre><code> $ ./gradlew distZip </code></pre>
or
<pre><code> $ gradle distZip </code></pre>

After the command do follwing to unzip the distZip:
<pre><code> unzip /build/distributions/change-vector-collector.zip </code></pre>

The executable file is in build/distributions/change-vector-collector/bin

If you have trouble to build using gradlew, enter
<pre><code>$ gradle wrap</code></pre>

 <br>

### Project components
1. making buggy training set
    a. -r --repo (get BIC to BBIC)
    1. description: collects BBIC from BIC, which is needed before gumvec collection. BBIC contains the information of changes of before BIC.
    2. example:
    <pre><code> ./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -r -u "https://github.com/haidaros/defects4j-math" -i "/Users/jihoshin/Downloads/" -o "/Users/jihoshin/Downloads/" </code></pre>
    b. -g --gumtree (BBIC to x and y)
    1. description: applies gumtree algorithm on BBICs and outputs change vectors and the information (label) file.
    2.example:
    <pre><code> ./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -g -u "https://github.com/apache/commons-math" -i "/Users/jihoshin/Downloads/" -o "/Users/jihoshin/Downloads/" </code></pre>
2. making clean training set
    a. -q --clean (repo to x and y)
    1. description: collects clean changes in a repo. This is used to collect clean train sets. 
    2. example:
    <pre><code> ./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -q -u "https://github.com/apache/commons-math" -i "/Users/jihoshin/Downloads/" -o "/Users/jihoshin/Downloads/" </code></pre>
3. making test set (combined)
    a. -a --all
    1. description: collects all changes (both buggy and clean) in a repo. This is used to collect test instances.
    2. example:
    <pre><code> ./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -a -u "https://github.com/apache/opennlp" -i "./assets/" -o "./assets/" </code></pre>
4. miscellaneous
    a. -z --zero
    1. description: remove empty vectors and key duplications.
    2. example:
    <pre><code> ./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -z -u "https://github.com/apache/tez" -i "./assets/rm_zero/out/" -o "./assets/rm_zero/out/out2/" </code></pre>
    b. -l --local
    1. description: loads BBIC from an existing file in the local system.
    2. example:
    <pre><code> ./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -l -u "https://github.com/apache/commons-math" -i "./assets/BBIC_commons-math.csv" </code></pre>-o "./assets/"
    c. -d --defects4j
    1. description: makes test files for defects4j specifically (different in collecting BIC).
    2. example:
    <pre><code>./ChangeVectorCollector/change-vector-collector/bin/change-vector-collector -d -u "https://github.com/apache/commons-csv" -i "./assets/d4j/" -o "./assets/d4j/" </code></pre>
