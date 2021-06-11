# Using groovy in Oracle Data Integrator (ODI)


**ODI_SwingBuilder.groovy**

In this file I'm creating a simple GUI to read extraction names from a third party tool. That tool provides an API. In the code you will find the different functions to read
the output of API calls and after just pick the information that is needed. This is the current architecture:


![architecture](https://github.com/eloilopes/ODI/blob/main/architecture.png)


The GUI has one combobox and two buttons. Extractions can have parameters, that we want to prepare future executions. We click on "Check Parameters", fill out the parameters
with the values we want and after we click "OK". When we click OK we will update a control table and at the same time creates ODI variables with the same parameter name and values:


![solution](https://github.com/eloilopes/ODI/blob/main/Solution.png)



**GenerateODIDataStoresAutomatically.groovy**

In this script, using the same concept of extractions what we do is to create ODI Datastores and Attributes. The DataStore is the extraction name and the 
attributes are parameters and extraction name as well.

In the code we don't create the model, we are using a existing one. We ODI APIs to find the model we want to use, create the datastore and attributes.

Example:

```a = getPar.getInputStream().getText()
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
                def model = varM.findByCode("XU")
                println("var " + model)
                //find datastore    
                varD = (IOdiDataStoreFinder)odiInstance.getTransactionalEntityManager().getFinder(OdiDataStore.class);
                dataStore = varD.findByName(extract, "XU")
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
  ```
