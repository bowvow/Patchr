import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

import android.widget.TextView;

/***
 * 
 * @author Vaibhav
 *
 */
public class SetTagFixTest {
	
	private String filename;
	private String className;
	private String methodName;
	private byte[] orignalBytes;
	private byte[] modifiedBytes;
	
	/*
	 * 
	 * @param filename
	 * @param className
	 * @param methodName
	 * @throws IOException
	 */
	public SetTagFixTest(String filename, String className, 
			String methodName) throws IOException{
		
		this.filename = filename;
		this.className = className;
		this.methodName = methodName;
		readClassFile();
	}
	
	/*
	 * 
	 * @throws IOException
	 */
	private void readClassFile() throws IOException{
		Path path = Paths.get(filename);
		orignalBytes = Files.readAllBytes(path);
	}
	
	/*
	 * 
	 * @throws IOException
	 */
	private void writeClassFile() throws IOException{
		Path path = Paths.get("output\\" + filename);
		Files.write(path, modifiedBytes);
	}
	
	/*
	 * 
	 * @author Vaibhav
	 *
	 */
	class SetTagPatch extends MethodEditor.Patch{

		@Override
		public void emitTo(Output w) {
			//emit the new setTag Instruction
			w.emit(LoadInstruction.make(Constants.TYPE_Object, 2));
			w.emit(LoadInstruction.make(Constants.TYPE_Object, 3));
			w.emit(Util.makeInvoke(TextView.class, "setTag", new Class[] {java.lang.Object.class} ));
		}
		
	}
	
	
	/***
	 * 
	 * @param data
	 * @return
	 */
	private int findSetTagOffset(MethodData data){
		int offset = 0;
		//get all instructions in method
		IInstruction[] instructions = data.getInstructions();
		for(IInstruction instruction : instructions){
			//if instruction is of type Invoke
			if(instruction.toString().startsWith("Invoke")){
				InvokeInstruction instr = (InvokeInstruction) instruction;
				//Check if Invoke statement invokes setTag
				if(instr.getMethodName().equals("setTag")){
					break; 
				}
			}
			offset++;
		}
		return offset;
	}
	
	/***
	 * 
	 * @throws InvalidClassFileException
	 * @throws IOException
	 */
	public void applySetTagPatch() 
			throws InvalidClassFileException, IOException{
		ClassReader reader = new ClassReader(orignalBytes);
		for(int i=0; i < reader.getMethodCount();i++){
			// Find the method containing incorrect setTag
			if(reader.getMethodName(i).equals(methodName)){
				//visit this method to instrument it
				ClassInstrumenter instrumenter = 
						new ClassInstrumenter(methodName, reader, null, true);
				MethodData data = instrumenter.visitMethod(i);
				
				//Now that we found the method and are visiting it
			 	//We will instrument the code here to fix the setTag invokation
				MethodEditor editor = new MethodEditor(data); 
				editor.beginPass();
				
				//find setTag offset where fix is needed
				int offset = this.findSetTagOffset(data);
				
				//add patches here
				for(int j = 1; j <= 3; j++){
					editor.replaceWith(offset-j, new  MethodEditor.Patch(){
	
						@Override
						public void emitTo(Output w) {
							//replace the Instruction with Nothing
						}
						
					});
				}
				editor.replaceWith(offset, new SetTagPatch());
				
				//apply the patches here
				editor.applyPatches(); editor.endPass();
				
				//verify the changes
				try {
			          (new Verifier(data)).verify();
			    } catch (Verifier.FailureException ex) {
			          System.out.println("Verification failed at instruction "
			              + ex.getOffset() + ": " + ex.getReason());
			    }
				
				//Now that the patch is applied, write the modoified byte code to a file
				ClassWriter writer = instrumenter.emitClass();
				modifiedBytes = writer.makeBytes(); 
				writeClassFile();
				break;
			}
		}
	}
	
	/***
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		
		try {
			//Create a set tag fix test
			SetTagFixTest test = 
					new SetTagFixTest("MainActivity.class","MainActivity","onCreate");
					
			//Apply Patch to the File
			test.applySetTagPatch();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidClassFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
