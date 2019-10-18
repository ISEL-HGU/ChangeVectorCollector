# ChangeVectorCollector

ChangeVectorCollector is a tool that uses GumTree to differentiate two code between a change.

### How it works:
1. Takes a csv file that holds target commits and the target changed line.
<br> You can use result of HGUISEL/BugPatchCollector to find difference when BIC occurs.
2. Blames the changed line to get the commit before it is changed.
3. Extracts the files that is before and after the change.
4. Compares the change of files using GumTree distribution from https://github.com/GumTreeDiff/gumtree
5. Difference of code is vectorized with the attribute counts of changes w.r.t AST node types.
6. The output file is in the form of arff format.
7. Different correlation coefficients can be computed to the change vectors.

<br>

# How to build: Gradle
<pre><code> $ ./gradlew distZip </code></pre>
or
<pre><code> $ gradle distZip </code></pre>

After the command do follwing to unzip the distZip:
<pre><code> unzip /build/distributions/change-vector-collector.zip </code></pre>

The executable file is in build/distributions/change-vector-collector/bin

If you have trouble to build using gradlew, enter
<pre><code>$ gradle wrap</code></pre>

 