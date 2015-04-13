
abstract class PatchrBase{
  def getNode(parent:Any,child:String):Any 
    = parent.asInstanceOf[Map[String,Any]](child)
    
  def create(json:Any):PatchrBase
}

class PatchrVariable(
    classTyp : String,
    val_num : String,
    val_name : String
    ) extends PatchrBase{
  var classType : String = classTyp
  var valnum : String = val_num
  var valname : String = val_name
  
  def create(json:Any):PatchrBase 
    = new PatchrLocation(
        getNode(json,"classType").asInstanceOf[String],
        getNode(json,"valnum").asInstanceOf[String],
        getNode(json,"valname").asInstanceOf[String]
      ).asInstanceOf[PatchrBase]
      
}

class PatchrMethodRef(
      classtype:String,
      methodname:String,
      args:List[PatchrVariable],
      obj:PatchrVariable
    ){
  var classType : String = classtype
  var methodName : String = methodname
  var arguments : List[PatchrVariable] = args
  var callee : PatchrVariable = obj
}

class PatchrLocation(
    javafilename:String,
    classname:String,
    methodname:String
    ){
  var javaFilename = javafilename
  var className = classname
  var methodName = methodname
}