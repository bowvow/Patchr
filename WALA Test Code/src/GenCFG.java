/**
 * 
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.ClassFileModule;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

/**
 * @author hqdvista
 * 
 */
public class GenCFG {

	/**
	 * @param args
	 * @throws ClassHierarchyException
	 * @throws IOException
	 * @throws CallGraphBuilderCancelException
	 * @throws IllegalArgumentException
	 * @throws InvalidClassFileException
	 */
	public static void main(String[] args) throws ClassHierarchyException,
			IOException, IllegalArgumentException,
			CallGraphBuilderCancelException, InvalidClassFileException {

		// initialize scope
		//ATTENTION: remember to replace the file paths
		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(
				"MainActivity.class", new File("excludes.txt"));
		scope.addToScope(ClassLoaderReference.Primordial, new JarFile(new File(
				"android.jar")));
		ClassHierarchy cha = ClassHierarchy.make(scope);

		// find entry point onCreate in MainActivity
		IClass c = cha.lookupClass(TypeReference.findOrCreate(
				ClassLoaderReference.Application,
				"Lcom/example/settagtest/MainActivity"));
		List<Entrypoint> entries = new ArrayList<Entrypoint>();
		Atom atom = Atom.findOrCreateUnicodeAtom("onCreate");
		Collection<IMethod> allMethods = c.getAllMethods();
		for (IMethod m : allMethods) {
			if (m.getName().equals(atom)
					&& (m.getDeclaringClass().getClassLoader().getReference()
							.getName().equals(Atom
							.findOrCreateUnicodeAtom("Application")))) {
				entries.add(new DefaultEntrypoint(m, cha));
			}
		}
		AnalysisOptions options = new AnalysisOptions(scope, entries);
		// //
		// build the call graph
		// //
		com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util
				.makeZeroCFABuilder(options, new AnalysisCache(), cha, scope,
						null, null);
		CallGraph cg = builder.makeCallGraph(options, null);
		//System.out.println(CallGraphStats.getStats(cg));

		Iterator<CGNode> iterator = cg.getEntrypointNodes().iterator();
		while (iterator.hasNext()) {
			CGNode cgNode = (CGNode) iterator.next();
			Iterator<CGNode> succnodeiter = cg.getSuccNodes(cgNode);
			//System.out.println(cgNode);

			while (succnodeiter.hasNext()) {
				CGNode succnode = (CGNode) succnodeiter.next();
				System.out.println(succnode);
				if(succnode.getMethod().getName().toString().equals("setTag")){
					if(succnode.getMethod().getNumberOfParameters() == 3){
						System.out.println("found invalid invokation of setTag in CG");
						SetTagSrcInfoTest info = new SetTagSrcInfoTest(cgNode);
						//info.findSrcLineNo();
						//info.findSrcFileName();
						//info.getIRInfo();
					}
				}
			}
		}

	}
}
