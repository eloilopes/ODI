import groovy.swing.SwingBuilder
import javax.swing.WindowConstants
import java.awt.FlowLayout as FL
import javax.swing.DefaultComboBoxModel
import javax.swing.BoxLayout as BXL
import javax.swing.JFrame
import java.awt.BorderLayout
import oracle.odi.domain.project.finder.IOdiProjectFinder;
import oracle.odi.domain.project.OdiProject;
import oracle.odi.domain.project.finder.IOdiFolderFinder;
import oracle.odi.domain.project.OdiFolder;
import oracle.odi.domain.project.finder.IOdiVariableFinder;
import oracle.odi.domain.topology.OdiTechnology
import oracle.odi.domain.model.finder.IOdiModelFinder
import oracle.odi.domain.model.finder.IOdiDataStoreFinder
import oracle.odi.domain.model.OdiModel
import oracle.odi.domain.model.OdiDataStore
import oracle.odi.domain.model.OdiColumn
import oracle.odi.domain.topology.finder.IOdiTechnologyFinder;
import oracle.odi.domain.topology.finder.IOdiDataServerFinder;
import oracle.odi.domain.topology.OdiDataServer
import oracle.odi.domain.topology.OdiPhysicalSchema
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

host = "IP or hostname and Port of your 3rd party server"
myExtractions=[]
ExtractionList = []

cont=0


def getExtractions(){
def get = new URL(host).openConnection();
			def getRC = get.getResponseCode();
			if(getRC.equals(200)) {
				a = get.getInputStream().getText()
				def myFile = new File('/home/oracle/Desktop/','ListExtractions.txt')
				myFile.write(a)
				//println myFile.text    
			}
			new File('/home/oracle/Desktop/', 'ListExtractions.txt').eachLine { line, nb ->
				if(nb == 1)
					return  
				l = line.substring(0,line.indexOf(','))
				ExtractionList.add(l)
                                //println "$l"
			}
  return ExtractionList
  } 

//Creates attributes based on parameters  
  def createAttribute(dataStore,name){
      OdiColumn attribute = new OdiColumn(dataStore, name)
      attribute.setDataTypeCode("VARCHAR2")
      attribute.setLength(10)
      tm.commit(txnStatus);
  }
  
 for (extract in getExtractions()){
 
 def getPar = new URL(host+"/config/extractions/"+extract+"/parameters").openConnection();
     
        def getRCPar = getPar.getResponseCode();

        if(getRCPar.equals(200)) {

            a = getPar.getInputStream().getText()
            filename= extract+'listParameters.txt'
            def myFilePar = new File('/home/oracle/Desktop/',filename)
            myFilePar.write(a)
           
            
            def jsonSlurper = new JsonSlurper()
            data = jsonSlurper.parse(new File('/home/oracle/Desktop/'+filename))  
            try{
            parameter = data.custom.name
            for (par in parameter){           
                txnDef = new DefaultTransactionDefinition();
                tm = odiInstance.getTransactionManager();
                tme = odiInstance.getTransactionalEntityManager()
                txnStatus = tm.getTransaction(txnDef);
                
                //Find Model
                varM = (IOdiModelFinder)odiInstance.getTransactionalEntityManager().getFinder(OdiModel.class);
                def model = varM.findByCode("<Model name>")
                println("var " + model)
                //find datastore    
                varD = (IOdiDataStoreFinder)odiInstance.getTransactionalEntityManager().getFinder(OdiDataStore.class);
                dataStore = varD.findByName(extract, "<Model name>")
              //If datastore doesn't exist create a new one
                if (dataStore == null){
                try{
                OdiDataStore myDS = new OdiDataStore(model, extract)
                if (cont == 0){
                  OdiColumn attribute = new OdiColumn(myDS, extract)
                  attribute.setDataTypeCode("VARCHAR2")
                  attribute.setLength(10)
                }
                createAttribute(myDS, par)
                
                } catch (Exception e){
                  println("Data Store already exists" +e)
                }
                }else{
                  try{    
                  createAttribute(dataStore, par)
                  } catch (Exception e){
                  println("Attribute already exists")
                }
                }
            cont++
			}
			}catch (Exception e){
              println("No parameters defined for this extraction "+ e) 
            }
        } 
	cont=0
 }
