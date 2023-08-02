


public interface LeaveAppApiConstants {
    String ENTITY_END_POINT = PRIVATE_API_ENDPOINT + "/leave-app";
    String GET_ALL_APPROVAL_STATUS = ENTITY_END_POINT + "/approval-status/get-all";

    String GET_All_LEAVE = ENTITY_END_POINT + "/get-all-leave";

    String GET_ALL_DOC_TYPE = ENTITY_END_POINT + "/doc-type/get-all";

    String GET_ALL_REGULAR_LEAVE_APP = ENTITY_END_POINT + "/regular/get-all";
   
    String GET_POSITIONS_TO_FORWARD = ENTITY_END_POINT + "/get-positions";
    
    String GET_LEAVE_APP_DOC = ENTITY_END_POINT + "/get-leave-app-doc";

    

    String DOC_APPROVAL_ACTION = ENTITY_END_POINT + "/doc-approval-action";

//    String SUBMIT_LEAVE_APP = ENTITY_END_POINT + "/submit/{id}";

    String SUBMIT_LEAVE_APP = ENTITY_END_POINT + "/submit";
    String RE_SUBMIT_LEAVE_APP = ENTITY_END_POINT + "/re-submit";
    String ON_CANCEL_LEAVE_APP = ENTITY_END_POINT + "/apply-for-cancel";

    String DELETE_LEAVE_APP = ENTITY_END_POINT + "/delete";
    String LEAVE_CHECK_APP = ENTITY_END_POINT + "/check";

    String LEAVE_CALENDER_LOAD = ENTITY_END_POINT + "/calender";

}
