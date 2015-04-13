import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IMethod.SourcePosition;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SymbolTable;


public class SetTagSrcInfoTest {
	
	private IClass Class;
	private IMethod method;
	private IR ir;
	
	public String srcFileName;
	public int srcLineNo;
	public String byteFileName;
	public int bytecodeIndex;
	
	public SetTagSrcInfoTest(CGNode node){
		ir = node.getIR();
		method = node.getMethod();
		Class = method.getDeclaringClass();
		
	}
	
	public void getIRInfo(){
		//System.out.println(ir.getParameterValueNumbers());
		int[] values = ir.getParameterValueNumbers();
	    for(int value : values){
	    	String[] localNames  = ir.getLocalNames(19, value);
		    for(String name : localNames)
		    	System.out.println(name);
	    }
	    
	    
	    SymbolTable table = ir.getSymbolTable();
	    System.out.println(table.getValue(11));
	}
	
	public void findSrcFileName(){
		srcFileName = Class.getSourceFileName();
		System.out.println(srcFileName);
	}

	public void findSrcLineNo() throws InvalidClassFileException{
		//SourcePosition srcPosition = method.getSourcePosition(0);
		
		int i = 0;
		IBytecodeMethod method = (IBytecodeMethod)ir.getMethod();
		IInstruction[] instructions = method.getInstructions();
		for(IInstruction instruction : instructions ){
			if(instruction.toString().contains(",setTag,")){
				System.out.println("found setTag in IR at " + i);
				break;
			}
			i++;
		}
	    bytecodeIndex = method.getBytecodeIndex(i);
	    srcLineNo = method.getLineNumber(bytecodeIndex);
	    System.out.println(bytecodeIndex + " " + srcLineNo);
	    
	    //print all instructions in method
	    //for(IInstruction instruction : instructions )
	    //	System.out.println(instruction.toString());
	    
	    
	}
	
}
