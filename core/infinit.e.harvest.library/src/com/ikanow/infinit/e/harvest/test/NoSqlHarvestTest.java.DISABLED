package com.ikanow.infinit.e.harvest.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.Table;

import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.MetadataBuilder;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.metadata.model.KunderaMetadata;
import com.impetus.kundera.metadata.model.PersistenceUnitMetadata;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;

public class NoSqlHarvestTest {

	/**
	 * @param args
	 * @throws CannotCompileException 
	 * @throws NotFoundException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings({ })
	public static void main(String[] args) throws CannotCompileException, NotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException {

		KunderaControlTest controlInstance = new KunderaControlTest();
		controlInstance.setIdField("control_id");
		controlInstance.setTestField("control_instance");
		
		Object instanceOfMyClass1 = modifyTemplateClassAndInstantiate(1);
		
		testKunderaInterface(controlInstance, instanceOfMyClass1);
		
		Object instanceOfMyClass2 = modifyTemplateClassAndInstantiate(2);
	
		testKunderaInterface(controlInstance, instanceOfMyClass2);		
	}

	private static Object modifyTemplateClassAndInstantiate(int nStep) throws NotFoundException, CannotCompileException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// 1] Class creation (http://stackoverflow.com/questions/3028304/javassist-annoations-problem?rq=1)
		
		// 1.1] Create a class
		
		ClassPool pool = ClassPool.getDefault();
		// Class have to have no package name for the JPQL to work, not clear why, presumably I missed a mapping somewhere
		// but it doesn't matter because this class is internal to the process space anyway
		CtClass cc = pool.makeClass("TemplateClass" + nStep);
		cc.detach();
		// Remove all fields:
		for (CtField toRemove: cc.getFields()) {
			cc.removeField(toRemove);
		}
		
		CtField testField = null;
		CtField testField2 = null;
		if (1 == nStep) {
			testField = new CtField(pool.get("java.lang.String"), "testField", cc);
			cc.addField(testField);
		}
		else {
			testField = new CtField(pool.get("java.lang.String"), "testField", cc);
			cc.addField(testField);
			
			testField2 = new CtField(pool.get("java.lang.String"), "testField2", cc);
			cc.addField(testField2);			
		}
		
		CtField idField = new CtField(pool.get("java.lang.String"), "idField", cc);
		cc.addField(idField);
		
		// 1.2] Annotations
		
		ClassFile ccFile = cc.getClassFile();
		ConstPool constpool = ccFile.getConstPool();
		// 1.2.1] Create an annotation for the class
		
		AnnotationsAttribute attrClass = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
		
		// 1.2.1.1] Entity
		Annotation annotEntity = new Annotation("javax.persistence.Entity", constpool);
		attrClass.addAnnotation(annotEntity);
		
		// 1.2.1.2] Table info
		Annotation annotTable = new Annotation("javax.persistence.Table", constpool);
		annotTable.addMemberValue("name", new StringMemberValue("TEST_CLASS", ccFile.getConstPool()));
		annotTable.addMemberValue("schema", new StringMemberValue("KunderaTest@mongoPU", ccFile.getConstPool()));		
		attrClass.addAnnotation(annotTable);
		
		// 1.2.1.3] Attach the annotation to the class
		
		ccFile.addAttribute(attrClass);
		
		// 1.2.2] Create an annotation for the field 		
		
		// 1.2.2.1] Annotation Id
		AnnotationsAttribute attrIdField = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
		
		Annotation annotId = new Annotation("javax.persistence.Id", constpool);
		attrIdField.addAnnotation(annotId);
		idField.getFieldInfo().addAttribute(attrIdField);
		
		// 1.2.2.2] Annotation Column (name="testField")
		AnnotationsAttribute attrTestField = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
		
		Annotation annotCol = new Annotation("javax.persistence.Column", constpool);
		annotCol.addMemberValue("name", new StringMemberValue("TEST_FIELD", ccFile.getConstPool()));
		attrTestField.addAnnotation(annotCol);
		testField.getFieldInfo().addAttribute(attrTestField);
		
		if (null != testField2) {
			AnnotationsAttribute attrTestField2 = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
			
			Annotation annotCol2 = new Annotation("javax.persistence.Column", constpool);
			annotCol.addMemberValue("name", new StringMemberValue("TEST_FIELD2", ccFile.getConstPool()));
			attrTestField2.addAnnotation(annotCol2);
			testField2.getFieldInfo().addAttribute(attrTestField2);
		}
		
		// 1.3] Add methods

		// example just to set a field
		if (cc.getConstructors().length > 0) {
			cc.removeConstructor(cc.getConstructors()[0]);
		}
		CtConstructor ccCon = null;
		if (1 == nStep) {
			ccCon = CtNewConstructor.make("public TemplateClass1() { idField = \"alex_id\"; testField = \"alex\"; }", cc);
		}
		else {
			ccCon = CtNewConstructor.make("public TemplateClass2() { idField = \"alex_id2\"; testField = \"alex\"; testField2 = \"alex2\"; }", cc);			
		}
		cc.addConstructor(ccCon);
		
		// 1.4] Interlude: print the class/annotation out to check it looks sensible
		
		System.out.println("Class Info: " + cc.toString());
		for (Object annotObj: cc.getAnnotations()) {
			System.out.println("Class Annotation: " + annotObj.toString());
		}
		for (Object annotObj: testField.getAnnotations()) {
			System.out.println("Field Annotation: " + annotObj.toString());
		}
		
		// 1.5] Create an instance
		
		// 1.5.1] (main test)
		Object instanceOfMyClass = null;
		
		instanceOfMyClass = cc.toClass().newInstance();
		return instanceOfMyClass;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void testKunderaInterface(KunderaControlTest controlInstance, Object instanceOfMyClass) {
		
		// 2] Integrate into Kundera (http://anismiles.wordpress.com/2010/07/14/kundera-now-jpa-1-0-compatible/)
		
		// 2.1] Create a persistence hash map (http://xamry.wordpress.com/2011/05/02/working-with-mongodb-using-kundera/)
		
		Map kunderaMongoConfig = new HashMap();
		kunderaMongoConfig.put("kundera.nodes", "dev.ikanow.com");
		kunderaMongoConfig.put("kundera.keyspace", "KunderaTest");
		
		// 2.2] Instantiate Kundera 
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("mongoPU", kunderaMongoConfig);
		EntityManager manager = factory.createEntityManager();

		// Add the metadata by hand to all the right places
		loadDynamicClassIntoKundera("mongoPU", instanceOfMyClass.getClass(), kunderaMongoConfig);
		
		// 2.3] Save an object
		
		//System.out.println("Persisting control instance..." + controlInstance.getClass().toString());
		//manager.persist(controlInstance);
		
		System.out.println("Persisting dynamically created instance..." + instanceOfMyClass.getClass().toString());
		manager.persist(instanceOfMyClass);
		
		System.out.println("Everything persisted!");
		
		// 2.4] Use JPQL/JPA (also test output as JSON)

		// Find by Id
		
		//KunderaControlTest x = manager.find(KunderaControlTest.class, "control_id");
		//System.out.println("CONTROL RESULT findOne: " + new com.google.gson.Gson().toJson(x));
		
		Object y = manager.find(instanceOfMyClass.getClass(), "alex_id");
		System.out.println("DYNAMIC RESULT findOne: " + new com.google.gson.Gson().toJson(y));
		
		String query = "Select p from " + instanceOfMyClass.getClass().getSimpleName() + " p where p.testField = alex";
		Query q = manager.createQuery(query);
		List results = q.getResultList();
		if (null != results) {
			System.out.println("Executed query '" +query + "': " + results.size());
			for (Object obj: results) {
				System.out.println("RESULT: " + new com.google.gson.Gson().toJson(obj));
			}
		}
		else {
			System.out.println("No results for query: " + query);
		}
		
		manager.close();
		factory.close();		
	}
	
	@SuppressWarnings("rawtypes")
	private static void loadDynamicClassIntoKundera(String persistenceUnit, Class<?> clazz, Map config) { 
		// Add the metadata by hand to all the right places
		// 0] Create the metadata:
		MetadataBuilder kunderaBuilder = new MetadataBuilder(persistenceUnit, (String) config.get("kundera.nodes"), config); 
		EntityMetadata myMeta = kunderaBuilder.buildEntityMetadata(clazz);
		// 1] class -> persitence unit:
		Map<String, List<String>> clazzHack = new HashMap<String, List<String>>();
		clazzHack.put(clazz.getName(), Arrays.asList(persistenceUnit)); 
		KunderaMetadata.INSTANCE.getApplicationMetadata().setClazzToPuMap(clazzHack);
		// 2] PU -> class:
		PersistenceUnitMetadata puMeta = KunderaMetadata.INSTANCE.getApplicationMetadata().getPersistenceUnitMetadata(persistenceUnit);
		puMeta.getClasses().add(clazz.getName());
		// 3] Classname -> class:
		KunderaMetadataManager.getMetamodel(persistenceUnit).addEntityNameToClassMapping(clazz.getName(), clazz);
		// 4] Add entity metadata
		// (note you can do this without the nasty MetamodelImpl by using KunderaMetadata.INSTANCE.getApplicationMetadata()
		//  but we'll do it like this for consistency with [3])
		KunderaMetadataManager.getMetamodel(persistenceUnit).addEntityMetadata(clazz, myMeta);
	}
	
	// (Just for double checking if the errors are javassist-related or Kundera-config related):
	
	@Entity
	@Table(name = "TEST_CLASS", schema = "KunderaTest@mongoPU")
	public static class KunderaControlTest {
		@Id
		private String idField;
		
		@Column(name="TEST_FIELD")
		private String testField;

		// Functions, no annotations needed
		public KunderaControlTest() {}
		
		public void setTestField(String testField) {
			this.testField = testField;
		}

		public String getTestField() {
			return testField;
		}

		public void setIdField(String idField) {
			this.idField = idField;
		}

		public String getIdField() {
			return idField;
		}
	}
	
}
