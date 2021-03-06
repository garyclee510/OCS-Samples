/** Program.java
 * 
 *  Copyright (C) 2018 OSIsoft, LLC. All rights reserved.
 * 
 *  THIS SOFTWARE CONTAINS CONFIDENTIAL INFORMATION AND TRADE SECRETS OF
 *  OSIsoft, LLC.  USE, DISCLOSURE, OR REPRODUCTION IS PROHIBITED WITHOUT
 *  THE PRIOR EXPRESS WRITTEN PERMISSION OF OSIsoft, LLC.
 * 
 *  RESTRICTED RIGHTS LEGEND
 *  Use, duplication, or disclosure by the Government is subject to restrictions
 *  as set forth in subparagraph (c)(1)(ii) of the Rights in Technical Data and
 *  Computer Software clause at DFARS 252.227.7013
 * 
 *  OSIsoft, LLC
 *  1600 Alvarado St, San Leandro, CA 94577
 */

package samples;

import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Program {
    // get configuration
    static String tenantId = getConfiguration("tenantId");
    static String namespaceId = getConfiguration("namespaceId");
    static String sdsServerUrl = getConfiguration("sdsServerUrl");	
	
    // id strings
    static String sampleTypeId = "WaveData_SampleType";
    static String targetTypeId = "WaveData_SampleTargetType";
    static String integerTargetTypeId = "WaveData_SampleIntegerTargetType";
    static String sampleStreamId = "WaveData_SampleStream";
    static String sampleViewId = "WaveData_SampleView";
    static String sampleManualViewId = "WaveData_SampleManualView";
    
    
    public static void main(String[] args) throws InterruptedException {
    	
        // Create Sds client to communicate with server
    	System.out.println("---------------------------------------------------");
        System.out.println("  _________    .___          ____.                    ");
        System.out.println(" /   _____/  __| _/______   |    |____ ___  _______   ");
        System.out.println(" \\_____  \\  / __ |/  ___/   |    \\__  \\\\  \\/ /\\__  \\  ");
        System.out.println(" /        \\/ /_/ |\\___ \\/\\__|    |/ __ \\\\   /  / __ \\_");
        System.out.println("/_______  /\\____ /____  >________(____  /\\_/  (____  /");
        System.out.println("        \\/      \\/    \\/              \\/           \\/ ");
        System.out.println("---------------------------------------------------");
        
        String server = sdsServerUrl + "/";
        SdsClient sdsclient = new SdsClient(server);
        System.out.println("Sds endpoint at " + server);
        System.out.println();

        try { 	          	
            // create a SdsType
        	System.out.println("Creating a SdsType");
            SdsType sampleType = getWaveDataType(sampleTypeId);
            String jsonType = sdsclient.createType(tenantId, namespaceId, sampleType);
            sampleType = sdsclient.mGson.fromJson(jsonType, SdsType.class);
            
            //create a SdsStream
            System.out.println("Creating a SdsStream");
            SdsStream sampleStream = new SdsStream(sampleStreamId, sampleTypeId);
            String jsonStream = sdsclient.createStream(tenantId, namespaceId, sampleStream);
            sampleStream = sdsclient.mGson.fromJson(jsonStream, SdsStream.class);
            // insert data
            System.out.println("Inserting data");
            
            // insert a single event
            WaveData evt = WaveData.next(1, 2.0, 0);
            sdsclient.insertValue(tenantId, namespaceId, sampleStreamId, sdsclient.mGson.toJson(evt));

            // insert a list of events
            List<WaveData> events = new ArrayList<WaveData>();
            for (int i = 2; i < 20; i += 2) {
                evt = WaveData.next(1, 2.0, i);
                events.add(evt);
                Thread.sleep(10);
            	}
            sdsclient.insertValues(tenantId, namespaceId, sampleStreamId, sdsclient.mGson.toJson(events));
            
            // get the last value in stream
            System.out.println("Getting latest event");
            String jsonSingleValue = sdsclient.getLastValue(tenantId, namespaceId, sampleStreamId);
            WaveData data = sdsclient.mGson.fromJson(jsonSingleValue, WaveData.class);
            System.out.println(data.toString());
            System.out.println();
            // get all values
            System.out.println("Getting all events");            
            String jsonMultipleValues = sdsclient.getWindowValues(tenantId, namespaceId, sampleStreamId, "0", "20");
            Type listType = new TypeToken<ArrayList<WaveData>>() {}.getType(); // necessary for gson to decode list of WaveData, represents ArrayList<WaveData> type
            ArrayList<WaveData> foundEvents = sdsclient.mGson.fromJson(jsonMultipleValues, listType);
            System.out.println("Total events found: " + foundEvents.size());
            dumpEvents(foundEvents);
            System.out.println();
            
            // update the first value
            System.out.println("Updating events");
            evt = WaveData.next(1, 1.0, 0);
            sdsclient.updateValue(tenantId, namespaceId, sampleStreamId, sdsclient.mGson.toJson(evt));

            // update existing values and add 10 new values using update
            List<WaveData> newEvents = new ArrayList<WaveData>();
            for (int i = 2; i < 40; i += 2) // note: update will replace a value if it already exists, and create one if it does not
            {
                WaveData newEvt = WaveData.next(1, 1.0, i);
                newEvents.add(newEvt);
                Thread.sleep(10); // sleep for a bit because WaveData.radians is based on clock
            }
            sdsclient.updateValues(tenantId, namespaceId, sampleStreamId, sdsclient.mGson.toJson(newEvents));

            // retrieve values
            System.out.println("Getting updated events");
            jsonMultipleValues = sdsclient.getWindowValues(tenantId, namespaceId, sampleStreamId, "0", "40");
            foundEvents = sdsclient.mGson.fromJson(jsonMultipleValues, listType);
            System.out.println("Total events found: " + foundEvents.size());
            dumpEvents(foundEvents);
            System.out.println();
            
            // replace the first value
            System.out.println("Replacing events");
            evt = WaveData.next(1, 0.5, 0);
            sdsclient.replaceValue(tenantId, namespaceId, sampleStreamId, sdsclient.mGson.toJson(evt));

            // replace the remaining values
            newEvents = new ArrayList<WaveData>();
            for (int i = 2; i < 20; i += 2) {
                WaveData newEvt = WaveData.next(1, 0.5, i);
                newEvents.add(newEvt);
                Thread.sleep(10);
            }
            sdsclient.replaceValues(tenantId, namespaceId, sampleStreamId, sdsclient.mGson.toJson(newEvents));

            // retrieve values again to see replaced values
            System.out.println("Getting replaced events");
            jsonMultipleValues = sdsclient.getWindowValues(tenantId, namespaceId, sampleStreamId, "0", "40");
            foundEvents = sdsclient.mGson.fromJson(jsonMultipleValues, listType);
            System.out.println("Total events found: " + foundEvents.size());
            dumpEvents(foundEvents);
            System.out.println();
            
   		 	// Property Overrides
            System.out.println("Property Overrides");
            System.out.println("Sds can interpolate or extrapolate data at an index location where data does not explicitly exist:");
            System.out.println();
   		 	listType = new TypeToken<ArrayList<WaveData>>() {}.getType(); 
   		 	jsonMultipleValues = sdsclient.getRangeValues(tenantId, namespaceId, sampleStreamId, "1", 0, 3, false, SdsBoundaryType.ExactOrCalculated);
   		 	foundEvents = sdsclient.mGson.fromJson(jsonMultipleValues, listType);
         
   		 	System.out.println("Default (Continuous) requesting data starting at index location '1', where we have not entered data, Sds will interpolate a value for each property:");
   		 	for (WaveData evnt : foundEvents) {
   		 		System.out.println("Order: " + evnt.getOrder() + ", Radians: " + evnt.getRadians() + ", Cos: " + evnt.getCos());
   		 	}
   		 	System.out.println();
         
   		 	// Create a Discrete stream PropertyOverride indicating that we do not want Sds to calculate a value for Radians and update our stream 
   		 	SdsStreamPropertyOverride propertyOverride = new SdsStreamPropertyOverride();
   		 	propertyOverride.setSdsTypePropertyId("Radians");
   		 	propertyOverride.setInterpolationMode(SdsInterpolationMode.Discrete);
   		 	List<SdsStreamPropertyOverride> propertyOverrides = new ArrayList<SdsStreamPropertyOverride>();
   		 	propertyOverrides.add(propertyOverride);
   		 	
			// update the stream
			sampleStream.setPropertyOverrides(propertyOverrides);
   		 	sdsclient.updateStream(tenantId, namespaceId, sampleStreamId, sampleStream);

   		 	// repeat the retrieval
   		 	jsonMultipleValues = sdsclient.getRangeValues(tenantId, namespaceId, sampleStreamId, "1", 0, 3, false, SdsBoundaryType.ExactOrCalculated);
   		 	foundEvents = sdsclient.mGson.fromJson(jsonMultipleValues, listType);
   		    System.out.println("We can override this behavior on a property by property basis, here we override the Radians property instructing Sds not to interpolate.");
   		    System.out.println("Sds will now return the default value for the data type:");
   		 	for (WaveData evnt : foundEvents) {
   		 		System.out.println("Order: " + evnt.getOrder() + ", Radians: " + evnt.getRadians() + ", Cos: " + evnt.getCos());
   		 	}
   		 	System.out.println();
         
   		 	// SdsViews 
   		 	System.out.println("SdsViews"); 
   		 	System.out.println("Here is some of our data as it is stored on the server:");
   		 	for (WaveData evnt : foundEvents) {
   		 		System.out.println("Sin: " + evnt.getSin() + ", Cos: " + evnt.getCos() + ", Tan" + evnt.getTan());            
   		 	}
   		 	System.out.println();
         
   		 	// create target SdsTypes         
   		 	SdsType targetType = getWaveDataTargetType(targetTypeId);
   		 	String jsonTargetType = sdsclient.createType(tenantId, namespaceId, targetType);
   		 	targetType = sdsclient.mGson.fromJson(jsonTargetType, SdsType.class);         
   		 	SdsType targetIntegerType = getWaveDataTargetIntegerType(integerTargetTypeId);
   		 	String jsonTargetIntegerType = sdsclient.createType(tenantId, namespaceId, targetIntegerType);
   		 	targetIntegerType = sdsclient.mGson.fromJson(jsonTargetIntegerType, SdsType.class);
         
   		 	// create a SdsView
   		 	SdsView autoView = new SdsView();
   		 	autoView.setId(sampleViewId);
   		 	autoView.setName("SampleAutoView");
   		 	autoView.setDescription("This is a view mapping SampleType to SampleTargetType");
   		 	autoView.setSourceTypeId(sampleTypeId);
   		 	autoView.setTargetTypeId(targetTypeId);
   		 	String jsonAutoView = sdsclient.createView(tenantId, namespaceId, autoView);
   		 	autoView = sdsclient.mGson.fromJson(jsonAutoView, SdsView.class);
                  
   		 	// create SdsViewProperties
   		 	SdsViewProperty vp1 = new SdsViewProperty();
   		 	vp1.setSourceId("Order");
   		 	vp1.setTargetId("OrderTarget");
         
   		 	SdsViewProperty vp2 = new SdsViewProperty();
   		 	vp2.setSourceId("Sin");
   		 	vp2.setTargetId("SinInt");
         
   		 	SdsViewProperty vp3 = new SdsViewProperty();
   		 	vp3.setSourceId("Cos");
   		 	vp3.setTargetId("CosInt");
         
   		 	SdsViewProperty vp4 = new SdsViewProperty();
   		 	vp4.setSourceId("Tan");
   		 	vp4.setTargetId("TanInt");
         
   		 	SdsViewProperty[] props = {vp1,vp2,vp3,vp4};
         
   		 	// create a SdsView with explicit SdsViewProperties         
   		 	SdsView manualView = new SdsView();
   		 	manualView.setId(sampleManualViewId);
   		 	manualView.setName("SampleManualView");
   		 	manualView.setDescription("This is a view mapping SampleType to SampleTargetType");
   		 	manualView.setSourceTypeId(sampleTypeId);
   		 	manualView.setTargetTypeId(integerTargetTypeId);
   		 	manualView.setProperties(props);
   		 	String jsonManualView = sdsclient.createView(tenantId, namespaceId, manualView);
   		 	manualView = sdsclient.mGson.fromJson(jsonManualView, SdsView.class);
         
   		 	// range values with automatically mapped SdsView
   		 	System.out.println("Specifying a view with a SdsType of the same shape returns values that are automatically mapped to the target SdsType's properties:");
   		 	Type targetListType = new TypeToken<ArrayList<WaveDataTarget>>() {}.getType(); 
   		 	jsonMultipleValues = sdsclient.getRangeValues(tenantId, namespaceId, sampleStreamId, "1", 0, 3, false, SdsBoundaryType.ExactOrCalculated, sampleViewId);
   		 	ArrayList<WaveDataTarget> foundTargetEvents = sdsclient.mGson.fromJson(jsonMultipleValues, targetListType);
   		 	for (WaveDataTarget evnt : foundTargetEvents) {
   		 		System.out.println("SinTarget: " + evnt.getSinTarget() + ", CosTarget: " + evnt.getCosTarget() + ", TanTarget: " + evnt.getTanTarget());            
   		 	}
   		 	System.out.println();

   		 	// range values with manually mapped SdsView
   		 	System.out.println("SdsViews can also convert certain types of data, here we return integers where the original values were doubles:");
   		 	Type integerListType = new TypeToken<ArrayList<WaveDataInteger>>() {}.getType(); 
   		 	jsonMultipleValues = sdsclient.getRangeValues(tenantId, namespaceId, sampleStreamId, "1", 0, 3, false, SdsBoundaryType.ExactOrCalculated, sampleManualViewId);
   		 	ArrayList<WaveDataInteger> foundIntegerEvents = sdsclient.mGson.fromJson(jsonMultipleValues, integerListType);
   		 	for (WaveDataInteger evnt : foundIntegerEvents) {
   		 		System.out.println("SinInt: " + evnt.getSinInt() + ", CosInt: " + evnt.getCosInt() + ", TanInt: " + evnt.getTanInt());            
   		 	}
   		 	System.out.println();
         
   		 	// SdsViewMaps
   		 	System.out.println("We can query Sds to return the SdsViewMap for our SdsView, here is the one generated automatically:");
   		 	Type sdsViewType = new TypeToken<SdsViewMap>() {}.getType(); 
   		 	String jsonViewMap = sdsclient.getViewMap(tenantId, namespaceId, sampleViewId);
   		 	SdsViewMap viewMap = sdsclient.mGson.fromJson(jsonViewMap, sdsViewType);
   		 	dumpSdsViewMap(viewMap);
   		 	System.out.println();
         
   		 	System.out.println("Here is our explicit mapping, note SdsViewMap will return all properties of the Source Type, even those without a corresponding Target property::");
   		 	sdsViewType = new TypeToken<SdsViewMap>() {}.getType(); 
   		 	jsonViewMap = sdsclient.getViewMap(tenantId, namespaceId, sampleManualViewId);
   		 	viewMap = sdsclient.mGson.fromJson(jsonViewMap, sdsViewType);
   		 	dumpSdsViewMap(viewMap);
   		 	System.out.println();
         
   		 	// tags, metadata and search
   		 	System.out.println("Let's add some Tags and Metadata to our stream:");
   		 	System.out.println();
   		 
   		 	ArrayList<String> tags = new ArrayList<String>();
   		 	tags.add("waves");
   		 	tags.add("periodic");
   		 	tags.add("2018");
   		 	tags.add("validated");
   		 	
   		 	Map<String, String> metadata = new HashMap<String, String>();
   		 	metadata.put("Region", "North America");
   		    metadata.put("Country", "Canada");
   		    metadata.put("Province", "Quebec");
   		    
   		    sdsclient.updateTags(tenantId, namespaceId, sampleStreamId, tags);
   		    sdsclient.updateMetadata(tenantId, namespaceId, sampleStreamId, metadata);
   		    
   		    System.out.println("Tags now associated with " + sampleStreamId);
   		    tags = sdsclient.getTags(tenantId, namespaceId, sampleStreamId);
   		    
   		    for (String tag: tags) {
		 		System.out.println(tag);
		 	}   		    
   		    System.out.println();
   		 	
   		    String region = sdsclient.getMetadata(tenantId, namespaceId, sampleStreamId, "Region");
   		    String country = sdsclient.getMetadata(tenantId, namespaceId, sampleStreamId, "Country"); 
   		    String province =  sdsclient.getMetadata(tenantId, namespaceId, sampleStreamId, "Province");
   		    
   		    System.out.println("Metadata now associated with " + sampleStreamId);
   		    System.out.println("Metadata key Region: " + region);
   		    System.out.println("Metadata key Country: " + country);
   		 	System.out.println("Metadata key Province: " + province);
   		    
   		 	System.out.println();
   		 	
   		 	// pause for search indexing
   		 	System.out.println("Pausing to allow for search indexing...");
   		 	TimeUnit.SECONDS.sleep(15);
   		 	
   		 	System.out.println("We can also use our tags to search for streams, let's search for streams tagged with 'periodic':");

   		 	ArrayList<SdsStream> streams = sdsclient.getStreams(tenantId, namespaceId, "periodic", "0", "100");
   		 	
   		 	for (SdsStream stream: streams) {
   		 		System.out.println("Found stream associated with 'periodic' with Id: " + stream.getId());
   		 	}
   		 	System.out.println();
   		 	
   		    // delete data
   		 	
   		 	// remove the first value
   		 	System.out.println("Deleting values from the SdsStream");
   		 	sdsclient.removeValue(tenantId, namespaceId, sampleStreamId, "0");

   		 	// remove remaining values
   		 	sdsclient.removeWindowValues(tenantId, namespaceId, sampleStreamId, "2", "40");

   		 	// retrieve values to check deletion
   		 	jsonMultipleValues = sdsclient.getWindowValues(tenantId, namespaceId, sampleStreamId, "0", "200");
   		 	foundEvents = sdsclient.mGson.fromJson(jsonMultipleValues, listType);
   		 	if(foundEvents.isEmpty())
   		 		System.out.println("All values deleted successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Cleaning up");          
                cleanUp(sdsclient);
                System.out.println("done");
            } catch (Exception e) {
                printError("Error deleting the Sds Objects", e);
            }
        }
    }

    /**
     * Returns the WaveData SdsType for the sample
     *
     * @param sampleTypeId - the id for the type
     * @return WaveData SdsType
     */
    private static SdsType getWaveDataType(String sampleTypeId) {
        SdsType intType = new SdsType();
        intType.setId("intType");
        intType.setSdsTypeCode(SdsTypeCode.Int32);

        SdsType doubleType = new SdsType();
        doubleType.setId("doubleType");
        doubleType.setSdsTypeCode(SdsTypeCode.Double);

        SdsTypeProperty orderProperty = new SdsTypeProperty();
        orderProperty.setId("Order");
        orderProperty.setSdsType(intType);
        orderProperty.setIsKey(true);

        SdsTypeProperty tauProperty = new SdsTypeProperty();
        tauProperty.setId("Tau");
        tauProperty.setSdsType(doubleType);

        SdsTypeProperty radiansProperty = new SdsTypeProperty();
        radiansProperty.setId("Radians");
        radiansProperty.setSdsType(doubleType);

        SdsTypeProperty sinProperty = new SdsTypeProperty();
        sinProperty.setId("Sin");
        sinProperty.setSdsType(doubleType);

        SdsTypeProperty cosProperty = new SdsTypeProperty();
        cosProperty.setId("Cos");
        cosProperty.setSdsType(doubleType);

        SdsTypeProperty tanProperty = new SdsTypeProperty();
        tanProperty.setId("Tan");
        tanProperty.setSdsType(doubleType);

        SdsTypeProperty sinhProperty = new SdsTypeProperty();
        sinhProperty.setId("Sinh");
        sinhProperty.setSdsType(doubleType);

        SdsTypeProperty coshProperty = new SdsTypeProperty();
        coshProperty.setId("cosh");
        coshProperty.setSdsType(doubleType);

        SdsTypeProperty tanhProperty = new SdsTypeProperty();
        tanhProperty.setId("Tanh");
        tanhProperty.setSdsType(doubleType);

        // Create a SdsType for our WaveData class; the metadata properties are the ones we just created
        SdsType type = new SdsType();
        type.setId(sampleTypeId);
        type.setName("WaveDataTypeJ");
        SdsTypeProperty[] props = {orderProperty, tauProperty, radiansProperty, sinProperty, cosProperty, tanProperty, sinhProperty, coshProperty, tanhProperty};
        type.setProperties(props);
        type.setSdsTypeCode(SdsTypeCode.Object);

        return type;
    }
    
    /**
     * Returns the WaveDataTarget SdsType for the sample
     *
     * @param sampleTypeId - the id for the type
     * @return WaveDataTarget SdsType
     */
    private static SdsType getWaveDataTargetType(String sampleTypeId) {
        SdsType intType = new SdsType();
        intType.setId("intType");
        intType.setSdsTypeCode(SdsTypeCode.Int32);

        SdsType doubleType = new SdsType();
        doubleType.setId("doubleType");
        doubleType.setSdsTypeCode(SdsTypeCode.Double);

        SdsTypeProperty orderTargetProperty = new SdsTypeProperty();
        orderTargetProperty.setId("OrderTarget");
        orderTargetProperty.setSdsType(intType);
        orderTargetProperty.setIsKey(true);

        SdsTypeProperty tauTargetProperty = new SdsTypeProperty();
        tauTargetProperty.setId("TauTarget");
        tauTargetProperty.setSdsType(doubleType);

        SdsTypeProperty radiansTargetProperty = new SdsTypeProperty();
        radiansTargetProperty.setId("RadiansTarget");
        radiansTargetProperty.setSdsType(doubleType);

        SdsTypeProperty sinTargetProperty = new SdsTypeProperty();
        sinTargetProperty.setId("SinTarget");
        sinTargetProperty.setSdsType(doubleType);

        SdsTypeProperty cosTargetProperty = new SdsTypeProperty();
        cosTargetProperty.setId("CosTarget");
        cosTargetProperty.setSdsType(doubleType);

        SdsTypeProperty tanTargetProperty = new SdsTypeProperty();
        tanTargetProperty.setId("TanTarget");
        tanTargetProperty.setSdsType(doubleType);

        SdsTypeProperty sinhTargetProperty = new SdsTypeProperty();
        sinhTargetProperty.setId("SinhTarget");
        sinhTargetProperty.setSdsType(doubleType);

        SdsTypeProperty coshTargetProperty = new SdsTypeProperty();
        coshTargetProperty.setId("CoshTarget");
        coshTargetProperty.setSdsType(doubleType);

        SdsTypeProperty tanhTargetProperty = new SdsTypeProperty();
        tanhTargetProperty.setId("TanhTarget");
        tanhTargetProperty.setSdsType(doubleType);

        // Create a SdsType for our WaveData class; the metadata properties are the ones we just created
        SdsType type = new SdsType();
        type.setId(sampleTypeId);
        type.setName("WaveDataTypeJ");
        SdsTypeProperty[] props = {orderTargetProperty, tauTargetProperty, radiansTargetProperty, sinTargetProperty, cosTargetProperty, tanTargetProperty, sinhTargetProperty, coshTargetProperty, tanhTargetProperty};
        type.setProperties(props);
        type.setSdsTypeCode(SdsTypeCode.Object);

        return type;
    	
    }

    /**
     * Returns the WaveDataInteger SdsType for the sample
     *
     * @param sampleTypeId - the id for the type
     * @return WaveDataInteger SdsType
     */    
    private static SdsType getWaveDataTargetIntegerType(String sampleTypeId) {
        SdsType intType = new SdsType();
        intType.setId("intType");
        intType.setSdsTypeCode(SdsTypeCode.Int32);

        SdsTypeProperty orderTargetProperty = new SdsTypeProperty();
        orderTargetProperty.setId("OrderTarget");
        orderTargetProperty.setSdsType(intType);
        orderTargetProperty.setIsKey(true);

        SdsTypeProperty sinIntProperty = new SdsTypeProperty();
        sinIntProperty.setId("SinInt");
        sinIntProperty.setSdsType(intType);

        SdsTypeProperty cosIntProperty = new SdsTypeProperty();
        cosIntProperty.setId("CosInt");
        cosIntProperty.setSdsType(intType);

        SdsTypeProperty tanIntProperty = new SdsTypeProperty();
        tanIntProperty.setId("TanInt");
        tanIntProperty.setSdsType(intType);
        
        // Create a SdsType for our WaveData class; the metadata properties are the ones we just created
        SdsType type = new SdsType();
        type.setId(sampleTypeId);
        type.setName("WaveDataTypeJ");
        SdsTypeProperty[] props = {orderTargetProperty, sinIntProperty, cosIntProperty, tanIntProperty};
        type.setProperties(props);
        type.setSdsTypeCode(SdsTypeCode.Object);

        return type;
    }
    
    /**
     * Prints out a formated error string
     *
     * @param exceptionDescription - the description of what the error is
     * @param exception            - the exception thrown
     */
    private static void printError(String exceptionDescription, Exception exception) {
        System.out.println("\n\n======= " + exceptionDescription + " =======");
        System.out.println(exception.toString());
        System.out.println("======= End of " + exceptionDescription + " =======");
    }

    private static void dumpEvents(ArrayList<WaveData> foundEvents) {
        for (WaveData evnt : foundEvents) {
            System.out.println(evnt.toString());
        }
    }
    
    private static void dumpSdsViewMap(SdsViewMap sdsViewMap) { 
        for (SdsViewProperty prop : sdsViewMap.getProperties()) {
            if(prop.getTargetId() != null)
           	 System.out.println(prop.getSourceId() + " => " + prop.getTargetId());
            else
           	 System.out.println(prop.getSourceId() + " => Not mapped");
        }
    }
    
    private static String getConfiguration(String propertyId) {
        String property = "";
        Properties props = new Properties();
        InputStream inputStream;

        try {
            System.out.println(new File(".").getAbsolutePath());
            inputStream = new FileInputStream("config.properties");
            props.load(inputStream);
            property = props.getProperty(propertyId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return property;
    }
	
    public static void cleanUp(SdsClient sdsclient) throws SdsError 
	{
		System.out.println("Deleting the stream");
        sdsclient.deleteStream(tenantId, namespaceId, sampleStreamId);
        System.out.println("Deleting the views");
        sdsclient.deleteView(tenantId, namespaceId, sampleViewId);
        sdsclient.deleteView(tenantId, namespaceId, sampleManualViewId);
        System.out.println("Deleting the types");
        sdsclient.deleteType(tenantId, namespaceId, sampleTypeId);
        sdsclient.deleteType(tenantId, namespaceId, targetTypeId);
        sdsclient.deleteType(tenantId, namespaceId, integerTargetTypeId);
	};
}
