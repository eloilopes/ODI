import groovy.swing.SwingBuilder
import javax.swing.WindowConstants
import java.awt.FlowLayout as FL
import javax.swing.DefaultComboBoxModel
import javax.swing.BoxLayout as BXL
import groovy.sql.Sql
import java.sql.Driver
import javax.swing.JFrame
import java.awt.BorderLayout
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;
import oracle.odi.domain.mapping.Mapping;
import oracle.odi.domain.mapping.component.Dataset;
import oracle.odi.domain.mapping.component.DatastoreComponent;
import oracle.odi.interfaces.interactive.support.InteractiveInterfaceHelperWithActions;
import oracle.odi.interfaces.interactive.IInteractiveInterfaceHelperWithActions;
import oracle.odi.core.persistence.transaction.ITransactionStatus;
import oracle.odi.core.persistence.transaction.support.DefaultTransactionDefinition;
import oracle.odi.domain.mapping.physical.MapPhysicalNode;
import groovy.json.JsonSlurper;
import groovy.beans.Bindable 
import oracle.odi.domain.project.StepVariable.DeclareVariable


def swing = new SwingBuilder()
host = "<IP or hostname and Port of your 3rd party server>"
myExtractions=[]
ExtractionList = []
d = new java.awt.Dimension(205,20)  

def arrayTextFields = []
def password
def parms = []
label = "label"

// Build list of Extractions
         def getExtractionsList (extractioname) {
			extractions = []
			new File('<OS path>/', 'response.txt').eachLine { line, nb ->
				if(nb == 1)
					return
				l = line.substring(0,line.indexOf(','))
				extractions.add(l)
			}
			updateParameters(extractioname)
			return extractions.find { it == extractioname }
        }
        
        def updateParameters(myExtractions) {
   
        def getPar = new URL(host+"<API>"+myExtractions+"/<parameters>").openConnection();
     
            def getRCPar = getPar.getResponseCode();

            if(getRCPar.equals(200)) {

                a = getPar.getInputStream().getText()
                def myFilePar = new File('<path>','listParameters.txt')
                myFilePar.write(a)
                println ("listParameters.txt generated ")  
            }

        }
 
 //Function that opens the connection and update the control tabe
     def updateControlTable(parameterValue, extractionName, parameterName)  {
     
        def driver = Class.forName('oracle.jdbc.OracleDriver').newInstance() as Driver
        def props = new Properties()
        props.setProperty("user", "<user>") 
        props.setProperty("password", "<password>")
        def conn = driver.connect("jdbc:oracle:thin:@<host>:1521/<service_name>", props) 
        def sql = new Sql(conn)
        sql.executeUpdate "update <table_name> set <parameter_column>=$parameterValue WHERE <parameter_name>=$parameterName and <extraction_name>=$extractionName"
        sql.close()
       
    }

    def frame = swing.frame(id: 'top', size:[500,200], title:'Xtract Server', layout:new BorderLayout()) {
      noparent() {
    
 
    
             //Open the connection to API and generates a file to read after
		 getExtractions = {
			def get = new URL(host).openConnection();
			def getRC = get.getResponseCode();
			if(getRC.equals(200)) {
				a = get.getInputStream().getText()
				def myFile = new File('<OS path>','response.txt')
				myFile.write(a)
 
			}
			new File('<OS path>', 'response.txt').eachLine { line, nb ->
				if(nb == 1)
					return  
				l = line.substring(0,line.indexOf(','))
				ExtractionList.add(l)

			}
                        
                      //Pick the first extraction by default
			updateParameters(ExtractionList[0])
        }
     
    // When we select the extraction on combobox we click on "Check Parameters" button and this function generates the textboxes and ODI variables
    //based on extraction parameters
    generateTextField = {    
          if (parms.size() >0) {
          try {
            for(parid in parms){
                  removeLabel = label+parid
                  swing.target.remove swing."$parid"
                  swing.target.remove swing."$removeLabel"
                  swing.doLater { swing.top.validate(); swing.top.repaint() }
                }
              } catch (e) {println("Expetion Here"+ e)  }  //do nothing if list is empty
              
            }
            def jsonSlurper = new JsonSlurper()
            data = jsonSlurper.parse(new File('<OS Path>/listParameters.txt'))           
            try{
            parameter = data.custom.name


            for (par in parameter){

            varf= (IOdiVariableFinder)odiInstance.getTransactionalEntityManager().getFinder(OdiVariable.class);
                panel(alignmentX:0f) {
                  flowLayout(alignment:FL.RIGHT)          
                  label(par)
                  parms << par
                  
                  swing.target.add swing.label( "$par", id: label+par)
                  a = swing.target.add swing.textField( "", id: par, columns:10)
                  def myGlobalExtractionName=varf.findGlobalByName("$par")
                  arrayTextFields << a
                  if (myGlobalExtractionName == null){
                    txnDef = new DefaultTransactionDefinition();
                    tm = odiInstance.getTransactionManager();
                    tme = odiInstance.getTransactionalEntityManager()
                    txnStatus = tm.getTransaction(txnDef);
                    myVar = new OdiVariable ("$par") 
                    odiInstance.getTransactionalEntityManager().persist(myVar)
                    tm.commit(txnStatus)
                    
                    
                  }
                  swing.doLater { swing.top.validate() }   
            }
            }
            }catch (Exception e){
              println("Exception "+ e) 
            }
        }
    
    
       // Function that reads the array with parameters and updates the variables
       //In the end updates the control table calling the function updateControlTable
       saveParameters =  {
       
       for (myVar in arrayTextFields){
         
            txnDef = new DefaultTransactionDefinition();
            tm = odiInstance.getTransactionManager();
            tme = odiInstance.getTransactionalEntityManager()
            txnStatus = tm.getTransaction(txnDef);
            varf= (IOdiVariableFinder)odiInstance.getTransactionalEntityManager().getFinder(OdiVariable.class);
              
             name=myVar.name  
             
             def myGlobalExtractionName=varf.findGlobalByName("$name")
             myGlobalExtractionName.setDefaultValue(swing."$name".text)  
             
             tm.commit(txnStatus);
             updateControlTable(swing."$name".text, "$myExtractions".toUpperCase(), "$name".toUpperCase())
             

         }
         swing.doLater { swing.top.dispose()}
         
       }
        
    }
      
     
   
    panel(id:'buttonPane',constraints:BorderLayout.NORTH) {
    txnDef = new DefaultTransactionDefinition();
    tm = odiInstance.getTransactionManager();
    tme = odiInstance.getTransactionalEntityManager()
    
    txnStatus = tm.getTransaction(txnDef);
    varf= (IOdiVariableFinder)odiInstance.getTransactionalEntityManager().getFinder(OdiVariable.class);
    
    // Getting all extractions   
    getExtractions()
    myExtractions = ExtractionList[0]

        label('Extraction Name:')
        comboBox(id:'extractioname', 
                    items:ExtractionList,
                    preferredSize:d,
                    itemStateChanged:{event->   
                      
                      myExtractions = getExtractionsList(extractioname.selectedItem)  })
        button( '<html>OK </html>', actionPerformed: saveParameters) 
        button( '<html>Check Parameters</html>', actionPerformed: generateTextField)  
        
     }
    
    
    panel(id:'target', constraints:BorderLayout.CENTER, size: [150,150]) {
        flowLayout()
        label( '')
    }
    
    
}

swing.doLater {
   frame.show() 

}
