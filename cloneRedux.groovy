// Initialize libraries
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.transform.BaseScript
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import java.util.Collection
import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.IssueFactory
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import java.sql.Timestamp
import com.atlassian.jira.config.SubTaskManager
import com.atlassian.jira.issue.AttachmentManager
import com.onresolve.scriptrunner.runner.util.UserMessageUtil
import com.atlassian.jira.project.version.VersionManager
@BaseScript CustomEndpointDelegate delegate
@JiraAgileBean
SprintIssueService sprintIssueService

//Initialize REST Endpoint
cloneRedux(groups: ["jira-users"]) { MultivaluedMap queryParams ->

// Get data from Clone Redux dialog
def issueId = queryParams.getFirst("issueId") as Long
def cloneSummary = queryParams.getFirst("cloneSummary") as String
IssueManager issueManager = ComponentAccessor.getComponent(IssueManager.class)
def issueKey = issueManager.getIssueObject(issueId).getKey() as String
def cloneSubTasks = queryParams.getFirst("cloneSubTasks") as String
def cloneAttachments = queryParams.getFirst("cloneAttachments") as String
def cloneIssueLinks = queryParams.getFirst("cloneIssueLinks") as String
def cloneSprintValues = queryParams.getFirst("cloneSprintValues") as String
def cloneDates = queryParams.getFirst("cloneDates") as String
def cloneFixVersions = queryParams.getFirst("cloneFixVersions") as String
def cloneAffectedVersions = queryParams.getFirst("cloneAffectedVersions") as String

// Initialize classes
IssueFactory issueFactory = ComponentAccessor.getComponent(IssueFactory.class)
IssueService issueService = ComponentAccessor.getComponent(IssueService.class)
IssueLinkManager issueLinkManager = ComponentAccessor.getComponent(IssueLinkManager.class)
IssueLinkTypeManager issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager.class)
SubTaskManager subTaskManager = ComponentAccessor.getComponent(SubTaskManager.class)
CustomField customField = ComponentAccessor.getComponent(CustomField.class)
CustomFieldManager customFieldManager = ComponentAccessor.getComponent(CustomFieldManager.class)
AttachmentManager attachmentManager  = ComponentAccessor.getComponent(AttachmentManager.class)
VersionManager versionManager  = ComponentAccessor.getComponent(VersionManager.class)
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
MutableIssue issue = issueManager.getIssueByCurrentKey(issueKey)

// Initialize the clone
def sourceIssue = issueFactory.cloneIssue(issue)

// Make a map of all the fields from the source issue
List<CustomField> allCustomFields = customFieldManager.getCustomFieldObjects(issue)
for (CustomField cf : allCustomFields) 
    {
        sourceIssue.setCustomFieldValue(cf, issue.getCustomFieldValue(cf))
    }
    
// Clear resolution
def resolutionDate = sourceIssue.getResolutionDate()
sourceIssue.setResolutionDate(null)

// Clear time spent
sourceIssue.setTimeSpent(null)
    
// Clear estimate
sourceIssue.setEstimate(sourceIssue.getOriginalEstimate())
    
// If clone dates is false, do not clone the dates
if (cloneDates.equals("false"))
    {
        def startDay = customFieldManager.getCustomFieldObject("customfield_15313")
        def endDay = customFieldManager.getCustomFieldObject("customfield_15314")
        def dueDate = sourceIssue.getDueDate()

        sourceIssue.setCustomFieldValue(startDay, null)
        sourceIssue.setCustomFieldValue(endDay, null)
        sourceIssue.setDueDate(null)
    }

// Get the summary from Clone Redux dialog
sourceIssue.setSummary(cloneSummary)
sourceIssue.setCreated(new Timestamp(System.currentTimeMillis()))

// Create the clone
def clonedIssue = issueManager.createIssueObject(currentUser, sourceIssue)

// If clone attachments is false, do not clone the attachments
if (cloneAttachments.equals("true"))
    {
        attachmentManager.copyAttachments(issue, currentUser, clonedIssue.getKey())
    }

// Get cloned issue key
MutableIssue destinationIssue = issueManager.getIssueByCurrentKey(clonedIssue.getKey())
    
// Set the parent link depending on the issue type (standard or sub-task)
def linkType
if (!issue.issueType.isSubTask()) // For standard issue type
    {
		linkType = issueLinkTypeManager.issueLinkTypes.findByName("Cloners")
		issueLinkManager.createIssueLink(destinationIssue.id, issue.id, linkType.id, 1L, currentUser)
    }
else // For sub-task
    {
        subTaskManager.createSubTaskIssueLink(issue.getParentObject(), destinationIssue, currentUser)
        linkType = issueLinkTypeManager.issueLinkTypes.findByName("Cloners")
		issueLinkManager.createIssueLink(destinationIssue.id, issue.id, linkType.id, 1L, currentUser)
    }
    
// If clone (non-system) issue links is false, do not clone the (non-system) issue links
if(cloneIssueLinks.equals("true"))
    { 
        issueLinkManager.getInwardLinks(issue.getId()).each{ inLinks ->
        switch(inLinks.getLinkTypeId().toString())
            {
                case "10100":
                break;
                case "10001":
                break;
                case "10800":
                break;
                case "10801":
                break;
                default:
                issueLinkManager.createIssueLink(inLinks.getSourceId(), destinationIssue.id, inLinks.getLinkTypeId(), 1L, currentUser)
                break;
            }
		}

		issueLinkManager.getOutwardLinks(issue.getId()).each{ outLinks ->
    	switch(outLinks.getLinkTypeId().toString())
        	{
                case "10100":
                break;
                case "10001":
                break;
                case "10800":
                break;
                case "10801":
                break;
                default:
                issueLinkManager.createIssueLink(destinationIssue.id, outLinks.getDestinationId(), outLinks.getLinkTypeId(), 1L, currentUser)
                break;
        	}
		}

    }
    
// If clone sprint values is false, do not clone the sprint values
if(cloneSprintValues.equals("false") && !issue.issueType.isSubTask())
    {
        sprintIssueService.getActiveOrFutureSprintForIssue(currentUser,issue).getValue().each{ everySprint ->
    	sprintIssueService.removeIssuesFromSprint(currentUser,everySprint as Sprint,[destinationIssue] as Collection)
		}
    }

// If clone fix versions is false, do not clone the fix versions
if (cloneFixVersions.equals("false"))
    {
        versionManager.updateIssueFixVersions(sourceIssue, null) 
    }
    
// If clone affected versions is false, do not clone the fix versions
if (cloneAffectedVersions.equals("false"))
    {
        versionManager.updateIssueAffectsVersions(sourceIssue, null) 
    }
    
// If clone sub-tasks is false, do not clone the sub-tasks
if(cloneSubTasks.equals("true") && !issue.issueType.isSubTask())
    {
        def subtasks = issue.getSubTaskObjects()
        def sourceSubTask, clonedSubTask
        subtasks.each {subtask ->
            sourceSubTask = issueFactory.cloneIssueWithAllFields(subtask)
            sourceSubTask.setSummary("CLONE - "+ subtask.getSummary())
            sourceSubTask.setResolutionDate(null)
            sourceSubTask.setTimeSpent(null)
            sourceSubTask.setEstimate(sourceSubTask.getOriginalEstimate())
            sourceSubTask.setCreated(new Timestamp(System.currentTimeMillis()))
            clonedSubTask = issueManager.createIssueObject(currentUser, sourceSubTask)
            subTaskManager.createSubTaskIssueLink(destinationIssue, clonedSubTask, currentUser)
        }  
    }

// Initialize the flag to be shown after cloning
def flag = [
	type: "success",      
    close: "auto", 
    title: 'Success',
    body: "Issue has been successfully cloned.", 
]
    
// Display the success flag after cloning
UserMessageUtil.flag(flag)

// Redirect to the cloned ticket
def redirectToNewIssue="""/browse/"""+destinationIssue+""""""
return Response.ok().type(MediaType.TEXT_HTML).entity(redirectToNewIssue.toString()).build()
}