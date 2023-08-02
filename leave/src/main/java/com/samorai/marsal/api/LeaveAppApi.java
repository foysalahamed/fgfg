

import javax.validation.Valid;
import javax.ws.rs.QueryParam;

@RestController
@Slf4j
public class LeaveAppApi {

    @Autowired
    private LeaveAppService leaveAppService;

    //TODO: Load Address
    @GetMapping(value = LeaveTypeApiConstants.LEAVE_GET_ADDRESS_DETAILS, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> loadAddressDetails( @RequestParam("type") String type, @RequestParam("empCode") String empCode, @RequestHeader("UserId") Long userId, @RequestHeader("Authorization") String token ) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_GET_ADDRESS_DETAILS),type,empCode);
        ResponseEntity<ResponseDto> response =  leaveAppService.getAddressByType(type, empCode, userId, token);
        log.info(String.format("Response returned for %s  : {}",LeaveTypeApiConstants.LEAVE_GET_ADDRESS_DETAILS), response);
        return response;
    }


    @GetMapping(value = LeaveTypeApiConstants.LEAVE_APP_GET_EMP_INFO_BY_BPID, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getBpGeneralInfo(@RequestParam("bpCode") String bpCode) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_APP_GET_EMP_INFO_BY_BPID),bpCode);
        ResponseEntity<ResponseDto> response =  leaveAppService.getBpGeneralInfo(bpCode);
        log.info(String.format("Response returned for %s  : {}", LeaveTypeApiConstants.LEAVE_APP_GET_EMP_INFO_BY_BPID), response);
        return  response;
    }

    @GetMapping(value = LeaveTypeApiConstants.LEAVE_APP_GET_BP_CODES,
            produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getBpCodes( @RequestHeader("UserId") Long userId,
                                                   @RequestParam(value = "searchCode", defaultValue = "") String searchCode,@RequestHeader("Authorization") String token) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_APP_GET_BP_CODES));
        ResponseEntity<ResponseDto> response = leaveAppService.getBpCodes(userId, searchCode,token);
        log.info(String.format("Response returned for %s  : {}", LeaveTypeApiConstants.LEAVE_APP_GET_BP_CODES), response);
        return response;
    }

    @GetMapping(value = LeaveTypeApiConstants.LEAVE_APP_GET_UNIT_WISE_BP_CODES,  produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getUnitWiseBpCodeList( @RequestHeader("Authorization") String token, @RequestHeader("UserId") Long userId, @RequestParam(value = "searchCode", defaultValue = "") String searchCode) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_APP_GET_UNIT_WISE_BP_CODES),searchCode);
        ResponseEntity<ResponseDto> response =  leaveAppService.getUnitWiseBpCode(token, userId, searchCode);
        log.info(String.format("Response returned for %s  : {}",LeaveTypeApiConstants.LEAVE_APP_GET_UNIT_WISE_BP_CODES), response);
        return response;
    }


    @GetMapping(value = LeaveTypeApiConstants.LEAVE_APP_GET_BY_ID, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getOne(@RequestParam("type") String type, @RequestParam("id") Long id) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_APP_GET_BY_ID),type,id);
        ResponseEntity<ResponseDto> response =  leaveAppService.getById(type, id);
        log.info(String.format("Response returned for %s  : {}", LeaveTypeApiConstants.LEAVE_APP_GET_BY_ID), response);
        return response;
    }

    @GetMapping(value = LeaveTypeApiConstants.LEAVE_APP_ALL_VALUE_GET_BY_ID, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getOne() throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_APP_ALL_VALUE_GET_BY_ID));
        ResponseEntity<ResponseDto> response =  leaveAppService.getAll();
        log.info(String.format("Response returned for %s  : {}", LeaveTypeApiConstants.LEAVE_APP_ALL_VALUE_GET_BY_ID), response);
        return response;
    }

    @PutMapping(value = LeaveTypeApiConstants.LEAVE_APP_VALUE_UPDATE, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> updateOne(@RequestBody LeaveAppDto leaveAppDto, @RequestHeader("UserId") Long userId) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveTypeApiConstants.LEAVE_APP_VALUE_UPDATE),leaveAppDto);
        ResponseEntity<ResponseDto> response =  leaveAppService.update(leaveAppDto, userId);
        log.info(String.format("Response returned for %s  : {}", LeaveTypeApiConstants.LEAVE_APP_VALUE_UPDATE), response);
        return response;
    }

    @PostMapping(value = ApiLeaveAppConstants.LEAVE_O_APP_SAVE, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> saveOne(@Valid @RequestBody LeaveAppDto leaveAppDto, @RequestHeader("UserId") Long userId) throws CommonException {
        log.info(String.format("Request received for employee info save: {}",ApiLeaveAppConstants.LEAVE_O_APP_SAVE), leaveAppDto);
        ResponseEntity<ResponseDto> response = leaveAppService.save(leaveAppDto, userId);
        log.info(String.format("Response return for employee info save: {}",ApiLeaveAppConstants.LEAVE_O_APP_SAVE), response);
        return response;
    }


    @GetMapping(value = ApiLeaveAppConstants.LEAVE_BALANCE, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getLeaveBalance(@QueryParam("userId") Long userId) {
        log.info(String.format("Request received for employee leave balance: {}",ApiLeaveAppConstants.LEAVE_BALANCE), userId);
        ResponseEntity<ResponseDto> response =  leaveAppService.getLeaveBalance(userId);
        log.info(String.format("Response returned for %s  : {}", ApiLeaveAppConstants.LEAVE_BALANCE), response);
        return response;
    }

    @GetMapping(value = ApiLeaveAppConstants.LEAVE_SPENDING, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getLeaveSpending() {
        log.info(String.format("Request received for %s  ", ApiLeaveAppConstants.LEAVE_SPENDING));
        ResponseEntity<ResponseDto> response = leaveAppService.getLeaveSpendingPlace();
        log.info(String.format("Response returned for %s  : {}", ApiLeaveAppConstants.LEAVE_SPENDING), response);
        return response;
    }

    @GetMapping(value = ApiLeaveAppConstants.LEAVE_CATEGORIES, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getLeaveTypesCategories() {
        log.info(String.format("Request received for %s  ", ApiLeaveAppConstants.LEAVE_CATEGORIES));
        ResponseEntity<ResponseDto> response =  leaveAppService.getLeaveCategories();
        log.info(String.format("Response returned for %s  : {}", ApiLeaveAppConstants.LEAVE_CATEGORIES), response);
        return response;
    }


    // MTH

    @GetMapping(value = LeaveAppApiConstants.GET_ALL_APPROVAL_STATUS, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getAllApprovalStatus() throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_ALL_APPROVAL_STATUS));
        ResponseEntity<ResponseDto> response = leaveAppService.getAllApprovalStatus();
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_ALL_APPROVAL_STATUS), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.GET_ALL_DOC_TYPE, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getAllDocType() throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_ALL_DOC_TYPE));
        ResponseEntity<ResponseDto> response = leaveAppService.getAllDocType();
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_ALL_DOC_TYPE), response);
        return response;
    }

    @PostMapping(value = LeaveAppApiConstants.GET_ALL_REGULAR_LEAVE_APP, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getAllLeaveApp(@RequestBody LeaveSearchCommonReqDto reqDto,
                                                      @RequestHeader("UserId") Long userId,
                                                      @RequestHeader("Authorization") String token) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_ALL_REGULAR_LEAVE_APP),reqDto);
        ResponseEntity<ResponseDto> response = leaveAppService.getAllLeaveApp(reqDto, userId, token);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_ALL_REGULAR_LEAVE_APP), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.GET_POSITIONS_TO_FORWARD, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getPositionsToForward(@RequestHeader("UserId") Long userId, @QueryParam("id") Long id) throws CommonException {
         log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_ALL_DOC_TYPE),id);
        ResponseEntity<ResponseDto> response = leaveAppService.getPositions(id == null ? userId : id);
         log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_ALL_DOC_TYPE), response);
        return response;
    }


    //  GET_LEAVE_APP_DOC
    @PostMapping(value = LeaveAppApiConstants.GET_LEAVE_APP_DOC, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getLeaveDoc(@RequestHeader("UserId") Long userId, @RequestBody DocSearchReqDto docSearchReqDto) throws CommonException {
         log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_ALL_DOC_TYPE),docSearchReqDto);
        ResponseEntity<ResponseDto> response = leaveAppService.getLeaveDoc(userId, docSearchReqDto);
         log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_ALL_DOC_TYPE), response);
        return response;
    }


    @PostMapping(value = LeaveAppApiConstants.DOC_APPROVAL_ACTION, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> updateDocumentStatus(  @RequestHeader("UserId") Long userId, @RequestHeader("Authorization") String token, @RequestBody DocApprovalReqDto ReqDto) throws CommonException {
         log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_ALL_DOC_TYPE),ReqDto);
        ResponseEntity<ResponseDto> response = leaveAppService.updateDocumentStatus(userId, ReqDto, token);
         log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_ALL_DOC_TYPE), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.SUBMIT_LEAVE_APP, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> submitLeaveApp(@QueryParam("id") Long id, @RequestHeader("UserId") Long userId) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.SUBMIT_LEAVE_APP),id);
        ResponseEntity<ResponseDto> response = leaveAppService.submitLeaveApp(id, userId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.SUBMIT_LEAVE_APP), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.RE_SUBMIT_LEAVE_APP, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> reSubmitLeaveApp(@QueryParam("id") Long id, @RequestHeader("UserId") Long userId) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.SUBMIT_LEAVE_APP),id);
        ResponseEntity<ResponseDto> response = leaveAppService.reSubmitLeaveApp(id, userId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.SUBMIT_LEAVE_APP), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.GET_LEAVE_DOC_DETAIL, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getLeaveDetail(@RequestParam("mstId") Long mstId) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_LEAVE_DOC_DETAIL),mstId);
        ResponseEntity<ResponseDto> response = leaveAppService.getLeaveDetail(mstId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_LEAVE_DOC_DETAIL), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.GET_LEAVE_EMP_INFO, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> getLeaveEmpInfo(@RequestParam("id") Long id) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_LEAVE_EMP_INFO),id);
        ResponseEntity<ResponseDto> response = leaveAppService.getLeaveEmpInfo(id);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_LEAVE_EMP_INFO), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.GET_OFVIS_CODE, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> fetchOfvisCodeList(@QueryParam("hrEmpId") Long hrEmpId) {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_OFVIS_CODE),hrEmpId);
        ResponseEntity<ResponseDto> response =  leaveAppService.fetchOfvisCode(hrEmpId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_OFVIS_CODE), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.GET_OFVIS_INFO, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> fetchOfvisInfo(@QueryParam("ofVisId") Long ofVisId) {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.GET_OFVIS_INFO),ofVisId);
        ResponseEntity<ResponseDto> response =  leaveAppService.fetchOfvisInfo(ofVisId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.GET_OFVIS_INFO), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.ON_CANCEL_LEAVE_APP, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> onLeaveCancel(@RequestParam("id") Long id, @RequestHeader("UserId") Long userId) throws CommonException {
        log.info(String.format("Request received for %s  ",LeaveAppApiConstants.ON_CANCEL_LEAVE_APP),id);
        ResponseEntity<ResponseDto> response =  leaveAppService.onLeaveCancel(id, userId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.ON_CANCEL_LEAVE_APP), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.DELETE_LEAVE_APP, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> onLeaveDelete(@QueryParam("id") Long id) throws CommonException {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.DELETE_LEAVE_APP),id);
        ResponseEntity<ResponseDto> response =  leaveAppService.onLeaveDelete(id);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.DELETE_LEAVE_APP), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.LEAVE_CHECK_APP, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> onLeaveDelete(LeaveCheck leaveCheck) throws CommonException  {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.LEAVE_CHECK_APP),leaveCheck);
        ResponseEntity<ResponseDto> response =  leaveAppService.onAvailableLeaveCheck(leaveCheck);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.LEAVE_CHECK_APP), response);
        return response;
    }

    @GetMapping(value = LeaveAppApiConstants.LEAVE_CALENDER_LOAD, produces = ApiConstants.EXTERNAL_MEDIA_TYPE)
    public ResponseEntity<ResponseDto> onCalendarLoad(@RequestHeader("UserId") Long userId, @QueryParam("hrEmpId") Long hrEmpId) throws CommonException  {
        log.info(String.format("Request received for %s  ", LeaveAppApiConstants.LEAVE_CALENDER_LOAD),hrEmpId);
        ResponseEntity<ResponseDto> response =  leaveAppService.getCalender(userId, hrEmpId);
        log.info(String.format("Response returned for %s  : {}", LeaveAppApiConstants.LEAVE_CALENDER_LOAD), response);
        return response;
    }
}
