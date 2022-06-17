/*******************************************************************************
 * Copyright 2018 T Mobile, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
/**
  Copyright (C) 2017 T Mobile Inc - All Rights Reserve
  Purpose: 
  Author :Amisha
  Modified Date: Jun 01, 2022
  
 **/
package com.tmobile.cloud.awsrules.dms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.amazonaws.util.StringUtils;
import com.tmobile.cloud.awsrules.utils.PacmanUtils;
import com.tmobile.cloud.constants.PacmanRuleConstants;
import com.tmobile.pacman.commons.PacmanSdkConstants;
import com.tmobile.pacman.commons.exception.InvalidInputException;
import com.tmobile.pacman.commons.exception.RuleExecutionFailedExeption;
import com.tmobile.pacman.commons.rule.Annotation;
import com.tmobile.pacman.commons.rule.BaseRule;
import com.tmobile.pacman.commons.rule.PacmanRule;
import com.tmobile.pacman.commons.rule.RuleResult;

@PacmanRule(key = "check-for-dms-encrypted-with-kms-cmks", desc = "checks for data migration services are encrypted using KMS Customer Master Keys", severity = PacmanSdkConstants.SEV_HIGH, category = PacmanSdkConstants.SECURITY)
public class DmsEncryptionUsingKMSCMKsRule extends BaseRule {

	private static final Logger logger = LoggerFactory.getLogger(DmsEncryptionUsingKMSCMKsRule.class);
	
	public static final String DEFAULT_KEY_MANAGER = "AWS";
    
	/**
	 * The method will get triggered from Rule Engine with following parameters
	 * 
	 * @param ruleParam
	 * 
	 ************** Following are the Rule Parameters********* <br><br>
	 * 
	 * ruleKey : check-for-dms-encrypted-with-kms-cmks <br><br>
	 *
	 * threadsafe : if true , rule will be executed on multiple threads <br><br>
	 *
	 * esKmsUrl : Enter the kms es api url <br><br>
	 *
	 * severity : Enter the value of severity <br><br>
	 * 
	 * ruleCategory : Enter the value of category <br><br>
	 * 
	 * @param resourceAttributes this is a resource in context which needs to be scanned this is provided by execution engine
	 *
	 */

	public RuleResult execute(final Map<String, String> ruleParam, Map<String, String> resourceAttributes) {

		logger.debug("========DmsEncryptionUsingKMSCMKsRule started=========");
		String esKmsUrl = null;
		Annotation annotation = null;

		String severity = ruleParam.get(PacmanRuleConstants.SEVERITY);
		String category = ruleParam.get(PacmanRuleConstants.CATEGORY);

		String formattedKmsUrl = PacmanUtils.formatUrl(ruleParam, PacmanRuleConstants.ES_KMS_URL);

		if (!StringUtils.isNullOrEmpty(formattedKmsUrl))
			esKmsUrl = formattedKmsUrl;

		MDC.put("executionId", ruleParam.get("executionId"));
		MDC.put("ruleId", ruleParam.get(PacmanSdkConstants.RULE_ID));

		LinkedHashMap<String, Object> issue = new LinkedHashMap<>();
		List<LinkedHashMap<String, Object>> issueList = new ArrayList<>();

		if (!PacmanUtils.doesAllHaveValue(severity, category, esKmsUrl)) {
			logger.info(PacmanRuleConstants.MISSING_CONFIGURATION);
			throw new InvalidInputException(PacmanRuleConstants.MISSING_CONFIGURATION);
		}

		if (resourceAttributes != null) {

			boolean isEncryptedWithKmsCmks = false;

			try {
				String kmsKeyId = resourceAttributes.get(PacmanRuleConstants.ES_KMS_KEY_ID_ATTRIBUTE);
				isEncryptedWithKmsCmks = PacmanUtils.checkIfResourceEncryptedWithKmsCmks(kmsKeyId, esKmsUrl, DEFAULT_KEY_MANAGER);
			} catch (Exception e) {
				logger.error("unable to determine", e);
				throw new RuleExecutionFailedExeption("unable to determine" + e);
			}

			if (!isEncryptedWithKmsCmks) {
				annotation = Annotation.buildAnnotation(ruleParam, Annotation.Type.ISSUE);
				annotation.put(PacmanSdkConstants.DESCRIPTION, "Data migration service is encrypted with AWS managed keys instead of Customer Master Keys !!");
				annotation.put(PacmanRuleConstants.SEVERITY, severity);
				annotation.put(PacmanRuleConstants.SUBTYPE, Annotation.Type.RECOMMENDATION.toString());
				annotation.put(PacmanRuleConstants.CATEGORY, category);
				annotation.put(PacmanRuleConstants.RESOURCE_ID, ruleParam.get(PacmanRuleConstants.RESOURCE_ID));
				issue.put(PacmanRuleConstants.VIOLATION_REASON, "Data migration service is encrypted with AWS managed keys instead of Customer Master Keys !!");
				issueList.add(issue);
				annotation.put("issueDetails", issueList.toString());
				logger.debug("========DmsEncryptionUsingKMSCMKsRule ended with annotation {} :=========", annotation);
				return new RuleResult(PacmanSdkConstants.STATUS_FAILURE, PacmanRuleConstants.FAILURE_MESSAGE, annotation);
			}

		}
		logger.debug("========DmsEncryptionUsingKMSCMKsRule ended=========");
		return new RuleResult(PacmanSdkConstants.STATUS_SUCCESS, PacmanRuleConstants.SUCCESS_MESSAGE);
	}

	public String getHelpText() {
		return "This rule checks for data migration services are encrypted using KMS Customer Master Keys";
	}
	
}
