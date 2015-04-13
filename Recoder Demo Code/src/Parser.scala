import scala.util.parsing.json._
import scala.collection.mutable.ListBuffer


object Patchr{
  var location : PatchrLocation = null
  var newmethodref : PatchrMethodRef = null
  var oldmethodref : PatchrMethodRef = null
  
  
  def processJsonInfo(JSONString:String){
    val json = JSON.parseFull(JSONString).get
    
    val root = getNode(json,"root")
    val fixes = getNode(root,"fixes").asInstanceOf[List[Any]]
    for(fix <- fixes){
      val bugType = getNode(fix,"bug fix type")
    
      if(bugType.equals("method ref replace")){
        doMethodRefReplace(fix)
      }
    }
    
  }
  
  def doMethodRefReplace(root:Any){
    location = getLocation(getNode(root,"location"))  
    newmethodref = getMethodRef(getNode(root,"newMethodRef"))
    oldmethodref = getMethodRef(getNode(root,"oldMethodRef"))
    
    new MethodReplace(
        "C:\\Users\\Vaibhav\\Documents\\MUSE\\eclipse\\Fixr\\workspace\\JSONTest\\input\\",
        location,newmethodref,oldmethodref
    ).processTransformation()
  }
  
  def getNode(parent:Any,child:String):Any 
    = parent.asInstanceOf[Map[String,Any]](child)
    
  def getLocation(json:Any):PatchrLocation = 
    new PatchrLocation(
        //getNode(json,"srcPath").asInstanceOf[String]
        getNode(json,"srcFilename").asInstanceOf[String],
        getNode(json,"className").asInstanceOf[String],
        getNode(json,"methodName").asInstanceOf[String]
      )
  
  def getVariable(json:Any):PatchrVariable = 
    new PatchrVariable(
        getNode(json,"type").asInstanceOf[String],
        getNode(json,"value num").asInstanceOf[String],
        getNode(json,"value name").asInstanceOf[String]
    )
  
  def getArguments(json:Any):List[PatchrVariable] = {
    val argList = new ListBuffer[PatchrVariable]()
    val jsonList = getNode(json,"arguments").asInstanceOf[List[Any]]
    for(item <- jsonList){
      val variable = getVariable(item)
      argList += variable
    }

    return argList.toList
  }
    
  
  def getMethodRef(json:Any) = 
    new PatchrMethodRef(
        getNode(json,"classType").asInstanceOf[String],
        getNode(json,"methodName").asInstanceOf[String],
        getArguments(json),
        getVariable(getNode(json,"callee"))
    )
}

object TestPatchr{
  def main(args:Array[String]){
 
    val jsonTestString = """
    {
      "root":{ "fixes":[
        {
          "bug fix type":"method ref replace",
  
          "location":{
              "srcFilename":"MainActivity.java",
              "className":"MainActivity",
              "methodName":"onCreate"
           },
  
          "oldMethodRef":{
              "classType":"widget.View",
              "methodName":"setTag",
              "arguments":[
                    {"type":"int", "value num":"1", "value name": "R.id.abc"},
                    {"type":"java.Object", "value num":"2", "value name": "tv"}
               ],
              "callee":{"type":"view.Widget", "value num":"2", "value name": "tv"}
          },
  
          "newMethodRef":{
              "classType":"widget.View",
              "methodName":"setTag", 
              "arguments":[
                    {"type":"java.util.ArrayList", "value num":"3", "value name": "x"}
               ],
              "callee":{"type":"view.Widget", "value num":"2", "value name": "tv"}
          }
        }
      ]}
    }
    """
    val json = JSON.parseFull(jsonTestString).get
    Patchr.processJsonInfo(jsonTestString)
  }
}