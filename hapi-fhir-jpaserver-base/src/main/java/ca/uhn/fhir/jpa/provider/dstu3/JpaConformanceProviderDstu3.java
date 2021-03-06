package ca.uhn.fhir.jpa.provider.dstu3;

/*
 * #%L
 * HAPI FHIR JPA Server
 * %%
 * Copyright (C) 2014 - 2018 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.CapabilityStatement.*;
import org.hl7.fhir.dstu3.model.Enumerations.SearchParamType;

import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.util.CoverageIgnore;
import ca.uhn.fhir.util.ExtensionConstants;

public class JpaConformanceProviderDstu3 extends org.hl7.fhir.dstu3.hapi.rest.server.ServerCapabilityStatementProvider {

	private volatile CapabilityStatement myCachedValue;
	private DaoConfig myDaoConfig;
	private String myImplementationDescription;
	private boolean myIncludeResourceCounts;
	private RestfulServer myRestfulServer;
	private IFhirSystemDao<Bundle, Meta> mySystemDao;
	
	/**
	 * Constructor
	 */
	@CoverageIgnore
	public JpaConformanceProviderDstu3(){
		super();
		super.setCache(false);
		setIncludeResourceCounts(true);
	}
	
	/**
	 * Constructor
	 */
	public JpaConformanceProviderDstu3(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig) {
		super(theRestfulServer);
		myRestfulServer = theRestfulServer;
		mySystemDao = theSystemDao;
		myDaoConfig = theDaoConfig;
		super.setCache(false);
		setIncludeResourceCounts(true);
	}

	@Override
	public CapabilityStatement getServerConformance(HttpServletRequest theRequest) {
		CapabilityStatement retVal = myCachedValue;

		Map<String, Long> counts = Collections.emptyMap();
		if (myIncludeResourceCounts) {
			counts = mySystemDao.getResourceCounts();
		}

		retVal = super.getServerConformance(theRequest);
		for (CapabilityStatementRestComponent nextRest : retVal.getRest()) {

			for (CapabilityStatementRestResourceComponent nextResource : nextRest.getResource()) {

				nextResource.setVersioning(ResourceVersionPolicy.VERSIONEDUPDATE);
				
				ConditionalDeleteStatus conditionalDelete = nextResource.getConditionalDelete();
				if (conditionalDelete == ConditionalDeleteStatus.MULTIPLE && myDaoConfig.isAllowMultipleDelete() == false) {
					nextResource.setConditionalDelete(ConditionalDeleteStatus.SINGLE);
				}

				// Add resource counts
				Long count = counts.get(nextResource.getTypeElement().getValueAsString());
				if (count != null) {
					nextResource.addExtension(new Extension(ExtensionConstants.CONF_RESOURCE_COUNT, new DecimalType(count)));
				}

				nextResource.getSearchParam().clear();
				String resourceName = nextResource.getType();
				RuntimeResourceDefinition resourceDef = myRestfulServer.getFhirContext().getResourceDefinition(resourceName);
				Collection<RuntimeSearchParam> searchParams = mySystemDao.getSearchParamsByResourceType(resourceDef);
				for (RuntimeSearchParam runtimeSp : searchParams) {
					CapabilityStatementRestResourceSearchParamComponent confSp = nextResource.addSearchParam();

					confSp.setName(runtimeSp.getName());
					confSp.setDocumentation(runtimeSp.getDescription());
					confSp.setDefinition(runtimeSp.getUri());
					switch (runtimeSp.getParamType()) {
					case COMPOSITE:
						confSp.setType(SearchParamType.COMPOSITE);
						break;
					case DATE:
						confSp.setType(SearchParamType.DATE);
						break;
					case NUMBER:
						confSp.setType(SearchParamType.NUMBER);
						break;
					case QUANTITY:
						confSp.setType(SearchParamType.QUANTITY);
						break;
					case REFERENCE:
						confSp.setType(SearchParamType.REFERENCE);
						break;
					case STRING:
						confSp.setType(SearchParamType.STRING);
						break;
					case TOKEN:
						confSp.setType(SearchParamType.TOKEN);
						break;
					case URI:
						confSp.setType(SearchParamType.URI);
						break;
					case HAS:
						// Shouldn't happen
						break;
					}
					
				}
				
			}
		}

		massage(retVal);
		
		retVal.getImplementation().setDescription(myImplementationDescription);
		myCachedValue = retVal;
		return retVal;
	}

	public boolean isIncludeResourceCounts() {
		return myIncludeResourceCounts;
	}
	
	/**
	 * Subclasses may override
	 */
	protected void massage(CapabilityStatement theStatement) {
		// nothing
	}

	public void setDaoConfig(DaoConfig myDaoConfig) {
		this.myDaoConfig = myDaoConfig;
	}

	@CoverageIgnore
	public void setImplementationDescription(String theImplDesc) {
		myImplementationDescription = theImplDesc;
	}

	public void setIncludeResourceCounts(boolean theIncludeResourceCounts) {
		myIncludeResourceCounts = theIncludeResourceCounts;
	}

	@Override
	public void setRestfulServer(RestfulServer theRestfulServer) {
		this.myRestfulServer = theRestfulServer;
		super.setRestfulServer(theRestfulServer);
	}

	@CoverageIgnore
	public void setSystemDao(IFhirSystemDao<Bundle, Meta> mySystemDao) {
		this.mySystemDao = mySystemDao;
	}
}
