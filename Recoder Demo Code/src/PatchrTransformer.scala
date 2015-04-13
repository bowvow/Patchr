
import recoder.CrossReferenceServiceConfiguration;
import recoder.ParserException;
import recoder.ProgramFactory;
import recoder.ServiceConfiguration;
import recoder.abstraction.ClassType;
import recoder.abstraction.Field;
import recoder.abstraction.Method;
import recoder.abstraction.Type;
import recoder.bytecode.ClassFile;
import recoder.convenience.AbstractTreeWalker;
import recoder.convenience.ForestWalker;
import recoder.convenience.Naming;
import recoder.io.ClassFileRepository;
import recoder.io.PropertyNames;
import recoder.io.SourceFileRepository;
import recoder.java.Comment;
import recoder.java.CompilationUnit;
import recoder.java.Expression;
import recoder.java.Identifier;
import recoder.java.JavaSourceElement;
import recoder.java.NonTerminalProgramElement;
import recoder.java.PackageSpecification;
import recoder.java.PrettyPrinter;
import recoder.java.ProgramElement;
import recoder.java.SourceElement;
import recoder.java.SourceVisitor;
import recoder.java.Statement;
import recoder.java.StatementBlock;
import recoder.java.declaration.ClassDeclaration;
import recoder.java.declaration.FieldDeclaration;
import recoder.java.declaration.FieldSpecification;
import recoder.java.declaration.MethodDeclaration;
import recoder.java.declaration.TypeArgumentDeclaration;
import recoder.java.declaration.TypeDeclaration;
import recoder.java.declaration.TypeDeclarationContainer;
import recoder.java.declaration.VariableSpecification;
import recoder.java.reference.MethodReference;
import recoder.java.reference.TypeReference;
import recoder.java.reference.UncollatedReferenceQualifier;
import recoder.kit.Problem;
import recoder.kit.ProblemReport;
import recoder.kit.Transformation;
import recoder.kit.TwoPassTransformation;
import recoder.list.generic.ASTArrayList;
import recoder.list.generic.ASTList;
import recoder.service.ProgramModelInfo;


class MethodReplace 
  (
      sourcePath : String,
      location : PatchrLocation,
      newMethodRef : PatchrMethodRef,
      oldMethodRef : PatchrMethodRef
  ) { 
   
  //save the arguments to class
  var newMethodRefference = newMethodRef
  var oldMethodRefference = oldMethodRef
  
  // Get the Service Configuration
  var serviceConfig : CrossReferenceServiceConfiguration
      = new CrossReferenceServiceConfiguration()
  
  // Get the file management service from the service config
  var srcRepo : SourceFileRepository = serviceConfig.getSourceFileRepository()
    
  // Get the program factory service
  var pfactory : ProgramFactory = serviceConfig.getProgramFactory()
    
  // Set Path for Java Files
  serviceConfig.getProjectSettings().setProperty(PropertyNames.INPUT_PATH, sourcePath)
  var path = sourcePath + location.javaFilename
  
  // Create compilation Unit for the java file
  var cu : CompilationUnit = srcRepo.getCompilationUnitFromFile(path)
  
  // old and new program Elements to be switched
  var oldStatement : Statement = null
  var newStatement : Statement = null
  
  //lookup
  def lookupClass(name : String) : TypeDeclaration = {
    val root : java.util.List[CompilationUnit] 
        = srcRepo.getAllCompilationUnitsFromPath()
    for(i <- 0 to root.size()){
      val c = root.get(i)
      for(i:Int <- 0 to c.getTypeDeclarationCount -1){
        val typ = c.getTypeDeclarationAt(i)
        if(typ.getName.equals(name)){
          return typ;
        }
      }
    }
    
    return null
  }
  
  
  
  // Find the Class Declaration in Source File
  def findClass(name : String) : TypeDeclaration = {
    
    for(i:Int <- 0 to cu.getTypeDeclarationCount -1){
      val typ = cu.getTypeDeclarationAt(i)
      if(typ.getName.equals(name)){
        return typ;
      }
    }
    
    return null
  }
  
  
  // Find the Method Declaration
  def findMethod(klass : TypeDeclaration, name : String) : MethodDeclaration = {
    for (i <- 0 to klass.getChildCount-1){
      val child = klass.getChildAt(i)
      if (child.isInstanceOf[MethodDeclaration]){
        val method = child.asInstanceOf[MethodDeclaration]
        if(method.getName.equals(name)){
          return method
        }
      }
    }
    
    return null
  }
  
  
  // Find Method Invocation
  def findMethodReference(method : MethodDeclaration){
    val list = method.getBody.getBody
    for(i <- 0 to list.size()-1){
      val statement = list.get(i)
      if(statement.isInstanceOf[MethodReference]){
        val ref = statement.asInstanceOf[MethodReference]
        
        if(isRequiredMethodRefStatement(ref)){
          oldStatement = ref
          println(oldStatement.toSource)
        }
      }
    }
  }
  
  
  //check if arguments for a method reference are same
  private def areSameArguments(ASTlist : ASTList[Expression], list : List[PatchrVariable]) : Boolean = {
    
    if(ASTlist.size() == list.size){
      for(i <- 0 to list.size -1){
        
        if(!Naming.toPathName(ASTlist.get(i)
            .asInstanceOf[UncollatedReferenceQualifier])
            .equals(list(i).valname)){
          return false
        }
      }
      // if did not return in for then all the names of args are same
      return true
    }
    
    return false
  }
  
  
  // check if Statement is the one we want.
  private def isRequiredMethodRefStatement(ref : MethodReference) : Boolean = {
    
    if(ref.getName.equals(oldMethodRef.methodName)
        && ref.getReferencePrefix
            .asInstanceOf[UncollatedReferenceQualifier].getName.equals(oldMethodRef.callee.valname)
        && areSameArguments(ref.getArguments, oldMethodRef.arguments)
       ){ 
      return true
    }
    
    return false
  }
  
  def createNewMethodRefernce() : MethodReference = {
    // Method Name
    val mname = pfactory.createIdentifier(newMethodRef.methodName)
    
    // Arguments
    //   create a new list
    val argList : ASTList[Expression] = new ASTArrayList[Expression]
    //   add the args to the list
    for (i <- 0 to newMethodRef.arguments.length-1){
      argList.add(
          pfactory.createUncollatedReferenceQualifier(
                             pfactory.createIdentifier(newMethodRef.arguments(i).valname))
                      )
    }
    
    // Callee
    val obj = pfactory.createUncollatedReferenceQualifier(
                        pfactory.createIdentifier(newMethodRef.callee.valname)  )
    
    val ref : MethodReference = pfactory.createMethodReference(obj, mname, argList)
    return ref
  }
  
  def makeAndSaveChange(outputfile: String) = {
    try{
      //make changes to cu
      (new ScalaTransformerWrapper(serviceConfig)).doReplaceWrapper(oldStatement, newStatement)
      println(cu.toSource())
      println(oldStatement.toSource())
      println(newStatement.toSource())
      //@TODO: Fix above code. Currently it throws exception if uncommented
      
      // Write changes back to file
      val transformedFile : java.io.Writer  = new java.io.FileWriter( "output\\" + outputfile );
      val pp : PrettyPrinter =
          pfactory.getPrettyPrinter(
              new java.io.PrintWriter(transformedFile)
          );
      cu.accept(pp);
      transformedFile.close();
      // Changes written to file
      
    } catch {
         case ex: java.io.FileNotFoundException => {
            println("Missing file exception")
         }
         case ex: java.io.IOException => {
            println("IO Exception")
         }
    }
    
  }
  
  def processTransformation(){
    val klass = findClass(location.className)
    
    if(klass != null){
      val method = findMethod(klass.asInstanceOf[TypeDeclaration], location.methodName)
      if(method != null){
        //println(method.asInstanceOf[MethodDeclaration].getName)
        findMethodReference(method)
      }
    }
    
    val argList : List[String] = List("x")
    newStatement = createNewMethodRefernce()
    
    makeAndSaveChange(location.javaFilename)
  }
  
}

  