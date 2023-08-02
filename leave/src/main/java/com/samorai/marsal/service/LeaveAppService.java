

import com.ibcs.ngpims.config.WebClientConfig;
import com.ibcs.ngpims.constants.ExternalApiSegments;
import com.ibcs.ngpims.dto.*;
import com.ibcs.ngpims.dto.leave.LeaveAppCLDto;
import com.ibcs.ngpims.dto.leave.LeaveCheck;
import com.ibcs.ngpims.exception.CommonException;
import com.ibcs.ngpims.model.*;
import com.ibcs.ngpims.repo.*;
import com.ibcs.ngpims.service.common.EmployeesGenInfo;
import com.ibcs.ngpims.service.common.LeaveBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.ibcs.ngpims.model.GlobalConfigType.LAP;
import static com.ibcs.ngpims.model.GlobalConfigType.LHAP;
import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
public class LeaveAppService {

    @Value("${webClient.baseUrl}")
    private String baseUrl;

    @Autowired
    private BaseLeaveAppRepo baseLeaveAppRepo;




    @Autowired
    private BaseLeaveAbroadAppRepo baseLeaveAbroadAppRepo;

    @Autowired
    private LeaveOthersAppRepo othersAppRepo;

    @Autowired
    private BaseLeaveClPlAppRepo baseLeaveClPlAppRepo;

    @Autowired
    private LeaveAppRepo leaveAppRepo;

    @Autowired
    private PositionRepo positionRepo;

    @Autowired
    private LeaveCurrStatusRepo leaveCurrStatusRepo;

    @Autowired
    private LeaveTypeRepo leaveTypeRepo;

    @Autowired
    private BLRepo blRepo;

    @Autowired
    private LookupRepo lookupRepo;

    @Autowired
    private OfvisAppRepo ofvisAppRepo;

    @Autowired
    private UnitRepo unitRepo;

    @Autowired
    private LeaveCalcPctRepo calcPctRepo;

    @Autowired
    private LeaveBalanceSummeryRepo balanceSummeryRepo;

    @Autowired
    private LeaveBalanceService balanceService;

    @Autowired
    private GlobalConfigurationRepo configurationRepo;

    @Autowired
    private CalendarRepo calendarRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Auditor getAuditor(Long userId) {
        Auditor auditor = new Auditor();
        auditor.setInsertBy(userId);
        auditor.setUpdateBy(userId);
        auditor.setInsertDate(LocalDateTime.now());
        auditor.setUpdateDate(LocalDateTime.now());
        return auditor;
    }

    public ResponseEntity<ResponseDto> fetchOfvisCode(Long empId) {
        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        ofvisAppRepo.getAll(empId)),
                HttpStatus.OK);
    }

    public ResponseEntity<ResponseDto> fetchOfvisInfo(Long ofVisId) {
        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        ofvisAppRepo.getOfvisInfoById(ofVisId)),
                HttpStatus.OK);
    }


    public ResponseEntity<ResponseDto> getAddressByType(String type, String code, Long userId, String token) throws CommonException {
        try {

            var result = WebClientConfig.getWebClient(userId, token)
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.HR_APP_GET_ADDRESS)
                                    .queryParam("type", type)
                                    .queryParam("empCode", code)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                            result),
                    HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getAddressByType ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }

    }

    public ResponseEntity<ResponseDto> getBpCodes(Long authUserId, String searchCode, String token) throws CommonException {

        try {

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", token)
                    .build();

            UnitResDto unitRes = webClient
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.GET_UNIT_ID_BY_USER_ID)
                                    .queryParam("userId", authUserId)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(UnitResDto.class)
                    .block();

            assert unitRes != null;
            UnitResSubDto payload = unitRes.getPayload();

            if (payload != null && payload.getUnitIds().size() > 0) {

                BpCodeResDto response = webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path(ExternalApiSegments.HR_APP_GET_BP_CODES)
                                        .queryParam("unitIds", payload.getUnitIds())
                                        .queryParam("searchCode", searchCode)
                                        .build()
                        )
                        .retrieve()
                        .bodyToMono(BpCodeResDto.class)
                        .block();
                String roleName = payload.getUserRole();
                BpCodeCompResDto mainResDto = new BpCodeCompResDto();


                if (roleName.equalsIgnoreCase("User")) {
                    assert response != null;
                    List<HrEmpMinResDto> filteredList = response.getPayload().stream()
                            .filter(obj -> Objects.equals(obj.getAuthUserId(), authUserId))
                            .collect(Collectors.toList());
                    mainResDto.setRoleName(roleName);
                    mainResDto.setBpCodes(filteredList);
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.OK,
                                    ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                    mainResDto),
                            HttpStatus.OK);
                } else {
                    mainResDto.setRoleName(roleName);
                    assert response != null;
                    mainResDto.setBpCodes(response.getPayload());
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.OK,
                                    ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                    mainResDto),
                            HttpStatus.OK);
                }
            } else {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.NOT_FOUND,
                                "Unit not found for current user!"),
                        HttpStatus.OK);
            }

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getBpCodes ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }


    }

    public ResponseEntity<ResponseDto> getUnitWiseBpCode(String token, Long authUserId, String searchCode) throws CommonException {

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("UserId", authUserId != null ? authUserId.toString() : "")
                    .defaultHeader("Authorization", token)
                    .build();

            UnitResDto unitRes = webClient
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.GET_UNIT_ID_BY_USER_ID)
                                    .queryParam("userId", authUserId)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(UnitResDto.class)
                    .block();

            assert unitRes != null;
            UnitResSubDto payload = unitRes.getPayload();

            if (payload != null && payload.getUnitIds().size() > 0) {

                BpCodeResDto response = webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path(ExternalApiSegments.HR_APP_GET_BP_CODES)
                                        .queryParam("unitIds", payload.getUnitIds())
                                        .queryParam("searchCode", searchCode)
                                        .build()
                        )
                        .retrieve()
                        .bodyToMono(BpCodeResDto.class)
                        .block();

                assert response != null;
                var filteredList = response.getPayload().stream()
                        .map(obj -> new DropdownDto(obj.getId(), obj.getCode(), obj.getCode()))
                        .collect(Collectors.toList());
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.OK,
                                ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                filteredList),
                        HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.NOT_FOUND,
                                "Unit not found for current user!"),
                        HttpStatus.OK);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getUnitWiseBpCode ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }


    }

    public ResponseEntity<ResponseDto> getBpGeneralInfo(String bpCode) throws CommonException {

        try {

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            BpGenInfoResDto response = webClient
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.HR_APP_GET_GEN_INFO)
                                    .queryParam("bpCode", bpCode)
                                    //.queryParam("searchCode", searchCode)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(BpGenInfoResDto.class)
                    .block();
            if (response != null) {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.OK,
                                ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                response.getPayload()),
                        HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.NOT_FOUND,
                                ResponseMessage.NOT_FOUND,
                                null),
                        HttpStatus.OK);
            }

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getBpGeneralInfo ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }

    }

    private ResponseEntity<ResponseDto> fetchClAndPL(Long id) throws CommonException {
        try{
        var leaveAppDto = new LeaveAppCLDto();
        leaveAppRepo.findById(id).ifPresent(dbObj -> {
            BeanUtils.copyProperties(dbObj, leaveAppDto, "approvedDate", "position");
            leaveAppDto.setId(dbObj.getId());
            leaveAppDto.setHrEmpId(dbObj.getHrEmpId());
            leaveAppDto.setApprovalStatus(dbObj.getApproved().getApprovalStatus().name());
            leaveAppDto.setAddressType(dbObj.getAddressType().name());
            leaveAppDto.setAppDate(dbObj.getApproved().getAppDate());
            leaveAppDto.setRemarks(dbObj.getApproved().getRemarks());
            leaveAppDto.setApprovedDate(dbObj.getApproved().getApprovedDate());
            leaveAppDto.setLeaveType(dbObj.getLeaveType().getId());
            leaveAppDto.setPosition(dbObj.getPosition().getId());
            leaveAppDto.setToPosition(dbObj.getToPosition().getId());
            leaveAppDto.setLeaveCat(dbObj.getLeaveType().getLeaveCat().name());
            leaveAppDto.setVersion(dbObj.getVersion());
            leaveAppDto.setEmergency(dbObj.isEmergency());
        });

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        leaveAppDto),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> fetchClAndPL ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> fetchCl_PL(Long id) throws CommonException{
        try{
        var leaveClPl = new LeaveAppClPlDto();
        baseLeaveClPlAppRepo.findById(id).ifPresent(dbObj -> {
            BeanUtils.copyProperties(dbObj, leaveClPl, "approvedDate", "position");
            leaveClPl.setId(dbObj.getId());
            leaveClPl.setPlFromDate(dbObj.getPlFromDate());
            leaveClPl.setPlToDate(dbObj.getPlToDate());
            leaveClPl.setApprovalStatus(dbObj.getApproved().getApprovalStatus().name());
            leaveClPl.setAddressType(dbObj.getAddressType().name());
            leaveClPl.setAppDate(dbObj.getApproved().getAppDate());
            leaveClPl.setRemarks(dbObj.getApproved().getRemarks());
            leaveClPl.setApprovedDate(dbObj.getApproved().getApprovedDate());
            leaveClPl.setLeaveType(dbObj.getLeaveType().getId());
            leaveClPl.setPosition(dbObj.getPosition().getId());
            leaveClPl.setToPosition(dbObj.getToPosition().getId());
            leaveClPl.setLeaveCat(dbObj.getLeaveType().getLeaveCat().name());
            leaveClPl.setVersion(dbObj.getVersion());
            leaveClPl.setEmergency(dbObj.isEmergency());
        });

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        leaveClPl),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> fetchCl_PL ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> fetchAbroad(Long id) throws CommonException{
        try{
        var leaveAbroad = new LeaveAppAbroadDto();
        baseLeaveAbroadAppRepo.findById(id).ifPresent(dbObj -> {
            BeanUtils.copyProperties(dbObj, leaveAbroad, "approvedDate", "position");
            leaveAbroad.setId(dbObj.getId());
            leaveAbroad.setApprovalStatus(dbObj.getApproved().getApprovalStatus().name());
            leaveAbroad.setHrEmpId(dbObj.getHrEmpId());
            leaveAbroad.setCountry(dbObj.getCountry().getId());
            leaveAbroad.setOfvisCode(dbObj.getOfvisCode());
            leaveAbroad.setAddressType(dbObj.getAddressType().name());
            leaveAbroad.setAppDate(dbObj.getApproved().getAppDate());
            leaveAbroad.setRemarks(dbObj.getApproved().getRemarks());
            leaveAbroad.setApprovedDate(dbObj.getApproved().getApprovedDate());
            leaveAbroad.setLeaveType(dbObj.getLeaveType().getId());
            leaveAbroad.setPosition(dbObj.getPosition().getId());
            leaveAbroad.setToPosition(dbObj.getToPosition().getId());
            leaveAbroad.setLeaveCat(dbObj.getLeaveType().getLeaveCat().name());
            leaveAbroad.setVersion(dbObj.getVersion());
            leaveAbroad.setEmergency(dbObj.isEmergency());
        });

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        leaveAbroad),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> fetchAbroad ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> fetchOther(Long id) throws CommonException {
        try{
        var tempDto = new LeaveAppOthersDto();
        othersAppRepo.findById(id).ifPresent(dbObj -> {
            BeanUtils.copyProperties(dbObj, tempDto, "approvedDate", "position");
            tempDto.setId(dbObj.getId());
            tempDto.setApprovalStatus(dbObj.getApproved().getApprovalStatus().name());
            tempDto.setHrEmpId(dbObj.getHrEmpId());
            tempDto.setAddressType(dbObj.getAddressType().name());
            tempDto.setAppDate(dbObj.getApproved().getAppDate());
            tempDto.setRemarks(dbObj.getApproved().getRemarks());
            tempDto.setApprovedDate(dbObj.getApproved().getApprovedDate());
            tempDto.setLeaveType(dbObj.getLeaveType().getId());
            tempDto.setPosition(dbObj.getPosition().getId());
            tempDto.setToPosition(dbObj.getToPosition().getId());
            tempDto.setLeaveCat(dbObj.getLeaveType().getLeaveCat().name());
            tempDto.setVersion(dbObj.getVersion());
            tempDto.setEmergency(dbObj.isEmergency());
        });

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        tempDto),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> fetchOther ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> getById(String type, Long id) throws CommonException {

        try {
            switch (type.toLowerCase()) {
                case "cl":
                case "pl":
                    return fetchClAndPL(id);
                case "cl_pl":
                    return fetchCl_PL(id);
                case "abroad":
                    return fetchAbroad(id);
                default:
                    return fetchOther(id);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> fetchOther ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> getAll() throws CommonException {

        try {
            List<LeaveAppDto> dtoList = new ArrayList<>();
            List<LeaveAppClPlDto> dtoClPlList = new ArrayList<>();

            List<LeaveApp> dbObjList = Streamable.of(leaveAppRepo.findAll()).toList();
            for (LeaveApp obj : dbObjList) {
                LeaveAppDto leaveAppDto = new LeaveAppDto();
                BeanUtils.copyProperties(obj, leaveAppDto);
                dtoList.add(leaveAppDto);
//                if(obj.getLeaveType().getId()==3){
//                    LeaveAppClPlDto leaveAppClPlDto = new LeaveAppClPlDto();
//                    BeanUtils.copyProperties(obj, leaveAppClPlDto);
//                    dtoClPlList.add(leaveAppClPlDto);
//
//
//
//                }else {
//                    BeanUtils.copyProperties(obj, leaveAppDto);
//                    dtoList.add(leaveAppDto);
//
//                }
            }

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                            dtoList),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> fetchOther ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private Lookup getLookup(Long id) {
        var lookup = lookupRepo.findById(id);
        return lookup.orElse(null);
    }

    private ResponseEntity<ResponseDto> SendOnlyCL(LeaveAppDto leaveAppDto, Long userId, int totalDays) throws CommonException {
        try{
        LeaveApp leaveApp = new LeaveApp();
        Long leaveType = leaveAppDto.getLeaveType();
        Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);

        toLevTyDb.ifPresent(leaveApp::setLeaveType);
        BeanUtils.copyProperties(leaveAppDto, leaveApp);
        Approved approver = new Approved();
        BeanUtils.copyProperties(leaveAppDto, approver);


        leaveApp.setTotalDays(totalDays);

        leaveApp.setAddressType(AddressType.valueOf(leaveAppDto.getAddressType()));
        Long fromPosId;
        var position = baseLeaveAppRepo.findPositions(leaveAppDto.getHrEmpId());
        if (position.isPresent()) {
            fromPosId = position.get().getEmpPosition();
            Optional<Position> fromPosDb = positionRepo.findById(fromPosId);
            if (fromPosDb.isPresent()) {
                var posId = fromPosDb.get().getId();
                var unitId = unitRepo.findUnitWiseLevel(posId, "LEVEL_2");
                unitRepo.findById(unitId).ifPresent(leaveApp::setLevelTwoUnit);

                leaveApp.setPosition(fromPosDb.get());
                var unitHeadPosition = unitRepo.unitHeadId(fromPosDb.get().getUnit().getId());
                if (unitHeadPosition.isPresent()) {
                    var headPosition = baseLeaveAppRepo.findPositions(unitHeadPosition.get());
                    var po = positionRepo.findById(headPosition.get().getEmpPosition());
                    po.ifPresent(obj -> approver.setPositionForwardedToId(po.get()));
                }
            }
        }

        Long toPosId = leaveAppDto.getToPosition();
        Optional<Position> toPosDb = positionRepo.findById(toPosId);

        if (toPosDb.isPresent()) {
            leaveApp.setToPosition(toPosDb.get());
        }

        leaveApp.setApproved(approver);
        leaveApp.setAuditor(getAuditor(userId));
        leaveApp.setEmpCode(leaveAppDto.getHrEmpCode());
        var religionList = leaveAppRepo.findReligion(leaveAppDto.getHrEmpId());
        if (religionList.size() > 0) {
            leaveApp.setReligionId(religionList.get(0).get(0));
        }
        leaveApp.setVersion(leaveAppDto.getVersion());
        leaveApp.setAuditor(getAuditor(userId));
        leaveApp = leaveAppRepo.saveAndFlush(leaveApp);
        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.CREATED,
                        ResponseMessage.SUCCESSFULLY_SAVED,
                        leaveApp.getId()),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> SendOnlyCL ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> sendClPl(LeaveAppDto leaveAppDto, Long userId) throws CommonException {

        BaseLeaveClPlApp baseLeaveClPlApp = new BaseLeaveClPlApp();

        try {

            Long leaveType = leaveAppDto.getLeaveType();
            int noOfDaysCLBetween = (int) DAYS.between(leaveAppDto.getFromDate(), leaveAppDto.getToDate()) + 1;
            int noOfDaysPLBetween = (int) DAYS.between(leaveAppDto.getPlFromDate(), leaveAppDto.getPlToDate()) + 1;
            Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);
            List<BaseLeaveClPlApp> alreadyExisCltList =
                    baseLeaveClPlAppRepo.checkIfExistForClHrEmpId(leaveAppDto.getHrEmpId(), leaveAppDto.getFromDate(), leaveAppDto.getToDate());
            if (leaveAppDto.getId() == null && !alreadyExisCltList.isEmpty()) {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.ALREADY_EXIST,
                                alreadyExisCltList.get(0).getId()),
                        HttpStatus.BAD_REQUEST);
            } else {
//                if (noOfDaysCLBetween <= 3 && noOfDaysPLBetween <= 2) {
                baseLeaveClPlApp.setLeaveType(toLevTyDb.get());
                BeanUtils.copyProperties(leaveAppDto, baseLeaveClPlApp);
                Approved approver = new Approved();
                BeanUtils.copyProperties(leaveAppDto, approver);
                baseLeaveClPlApp.setAddressType(AddressType.valueOf(leaveAppDto.getAddressType()));
                baseLeaveClPlApp.setTotalClDays(noOfDaysCLBetween);
                baseLeaveClPlApp.setTotalPlDays(noOfDaysPLBetween);
                baseLeaveClPlApp.setTotalDays(noOfDaysCLBetween + noOfDaysPLBetween);


                Long fromPosId;
                var position = leaveAppRepo.findPositions(leaveAppDto.getHrEmpId());
                if (position.size() > 0) {
                    fromPosId = position.get(0).get(0);
                    Optional<Position> fromPosDb = positionRepo.findById(fromPosId);
                    if (fromPosDb.isPresent()) {
                        var unitId = unitRepo.findUnitWiseLevel(fromPosDb.get().getId(), "LEVEL_2");
                        unitRepo.findById(unitId).ifPresent(baseLeaveClPlApp::setLevelTwoUnit);

                        baseLeaveClPlApp.setPosition(fromPosDb.get());
                        var unitHeadPosition = unitRepo.unitHeadId(fromPosDb.get().getUnit().getId());
                        if (unitHeadPosition.isPresent()) {
                            var headPosition = baseLeaveAppRepo.findPositions(unitHeadPosition.get());
                            var po = positionRepo.findById(headPosition.get().getEmpPosition());
                            po.ifPresent(obj -> approver.setPositionForwardedToId(po.get()));
                        }
                    }
                }

                Long toPosId = leaveAppDto.getToPosition();
                Optional<Position> toPosDb = positionRepo.findById(toPosId);
                if (toPosDb.isPresent()) {
                    baseLeaveClPlApp.setToPosition(toPosDb.get());
                }

                baseLeaveClPlApp.setApproved(approver);

                baseLeaveClPlApp.setAuditor(getAuditor(userId));
                baseLeaveClPlApp.setEmpCode(leaveAppDto.getHrEmpCode());
                var religionList = leaveAppRepo.findReligion(leaveAppDto.getHrEmpId());
                if (religionList.size() > 0) {
                    baseLeaveClPlApp.setReligionId(religionList.get(0).get(0));
                }
                baseLeaveClPlApp.setAuditor(getAuditor(userId));
                baseLeaveClPlApp = baseLeaveClPlAppRepo.save(baseLeaveClPlApp);
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.CREATED,
                                ResponseMessage.SUCCESSFULLY_SAVED,
                                baseLeaveClPlApp.getId()),
                        HttpStatus.OK);
//                }
//                return new ResponseEntity<>(
//                        new ResponseDto<>(
//                                ResponseStatus.FAILED,
//                                ResponseMessage.ERROR_MESSAGE,
//                                noOfDaysCLBetween),
//                        HttpStatus.BAD_REQUEST);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> sendClPl ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> sendClAndPL(LeaveAppDto leaveAppDto, Long userId) throws CommonException {
        LeaveApp leaveApp = new LeaveApp();
        Long leaveType = leaveAppDto.getLeaveType();
        int noOfDaysCLBetween = (int) (DAYS.between(leaveAppDto.getFromDate(), leaveAppDto.getToDate()) + 1);

        try {
            Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);
            List<LeaveApp> alreadyExistList = leaveAppRepo.checkIfExistForHrEmpId(leaveAppDto.getHrEmpId(), leaveAppDto.getFromDate(), leaveAppDto.getToDate());
            if (leaveAppDto.getId() == null && !alreadyExistList.isEmpty()) {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.ALREADY_EXIST,
                                alreadyExistList.get(0).getId()),
                        HttpStatus.BAD_REQUEST);
            } else {
                //CL Leave
                if (toLevTyDb.get().getCode().equals("CL")) {
                    if (noOfDaysCLBetween <= 10) {
                        return SendOnlyCL(leaveAppDto, userId, noOfDaysCLBetween);
                    }
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.FAILED,
                                    ResponseMessage.ERROR_MESSAGE,
                                    noOfDaysCLBetween),
                            HttpStatus.BAD_REQUEST);
                }

                // PL Leave
                leaveApp.setLeaveType(toLevTyDb.get());
                BeanUtils.copyProperties(leaveAppDto, leaveApp);

                leaveApp.setAddressType(AddressType.valueOf(leaveAppDto.getAddressType()));
                Long fromPosId;
                Optional<Long> unitHeadPosition;


                Long toPosId = leaveAppDto.getToPosition();
                Optional<Position> toPosDb = positionRepo.findById(toPosId);
                Approved approver = new Approved();
                BeanUtils.copyProperties(leaveAppDto, approver);

                var position = leaveAppRepo.findPositions(leaveAppDto.getHrEmpId());
                if (position.size() > 0) {
                    fromPosId = position.get(0).get(0);
                    Optional<Position> fromPosDb = positionRepo.findById(fromPosId);
                    if (fromPosDb.isPresent()) {
                        var unitId = unitRepo.findUnitWiseLevel(fromPosDb.get().getId(), "LEVEL_2");
                        unitRepo.findById(unitId).ifPresent(leaveApp::setLevelTwoUnit);

                        leaveApp.setPosition(fromPosDb.get());
                        unitHeadPosition = unitRepo.unitHeadId(fromPosDb.get().getUnit().getId());
                        if (unitHeadPosition.isPresent()) {
                            var headPosition = baseLeaveAppRepo.findPositions(unitHeadPosition.get());
                            var po = positionRepo.findById(headPosition.get().getEmpPosition());
                            po.ifPresent(obj -> approver.setPositionForwardedToId(po.get()));
                        }
                    }
                }

                if (toPosDb.isPresent()) {
                    leaveApp.setToPosition(toPosDb.get());
                }


                leaveApp.setApproved(approver);
                leaveApp.setAuditor(getAuditor(userId));
                leaveApp.setTotalDays(noOfDaysCLBetween);
                leaveApp.setEmpCode(leaveAppDto.getHrEmpCode());
                var religionList = leaveAppRepo.findReligion(leaveAppDto.getHrEmpId());
                if (religionList.size() > 0) {
                    leaveApp.setReligionId(religionList.get(0).get(0));
                }
                leaveApp.setVersion(leaveAppDto.getVersion());
                leaveApp.setAuditor(getAuditor(userId));
                leaveApp = leaveAppRepo.save(leaveApp);
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.CREATED,
                                ResponseMessage.SUCCESSFULLY_SAVED,
                                leaveApp.getId()),
                        HttpStatus.OK);

            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> sendClAndPL ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> SendEl(LeaveAppDto leaveAppDto, Long userId) throws CommonException {
        BaseLeaveAbroadApp baseLeaveAbroadApp = new BaseLeaveAbroadApp();
        try {

            Long leaveType = leaveAppDto.getLeaveType();
            Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);
            List<BaseLeaveAbroadApp> alreadyExistCltList =
                    baseLeaveAbroadAppRepo.checkIfExistForClHrEmpId(leaveAppDto.getHrEmpId(), leaveAppDto.getFromDate(), leaveAppDto.getToDate());
            if (leaveAppDto.getId() == null && !alreadyExistCltList.isEmpty()) {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.ALREADY_EXIST,
                                alreadyExistCltList.get(0).getId()),
                        HttpStatus.BAD_REQUEST);
            } else {

                int noOfDaysCLBetween = (int) (DAYS.between(leaveAppDto.getFromDate(), leaveAppDto.getToDate()) + 1);

                baseLeaveAbroadApp.setLeaveType(toLevTyDb.get());
                BeanUtils.copyProperties(leaveAppDto, baseLeaveAbroadApp);
                Approved approver = new Approved();

                BeanUtils.copyProperties(leaveAppDto, approver);

                baseLeaveAbroadApp.setAddressType(AddressType.valueOf(leaveAppDto.getAddressType()));
                baseLeaveAbroadApp.setOfvisCode(leaveAppDto.getOfvisCode());
                baseLeaveAbroadApp.setCountry(getLookup(leaveAppDto.getCountry()));

                baseLeaveAbroadApp.setTotalDays(noOfDaysCLBetween);

                Long fromPosId;
                var position = leaveAppRepo.findPositions(leaveAppDto.getHrEmpId());
                if (position.size() > 0) {
                    fromPosId = position.get(0).get(0);
                    Optional<Position> fromPosDb = positionRepo.findById(fromPosId);
                    if (fromPosDb.isPresent()) {
                        var unitId = unitRepo.findUnitWiseLevel(fromPosDb.get().getId(), "LEVEL_2");
                        unitRepo.findById(unitId).ifPresent(baseLeaveAbroadApp::setLevelTwoUnit);

                        baseLeaveAbroadApp.setPosition(fromPosDb.get());
                        var unitHeadPosition = unitRepo.unitHeadId(fromPosDb.get().getUnit().getId());
                        if (unitHeadPosition.isPresent()) {
                            var headPosition = baseLeaveAppRepo.findPositions(unitHeadPosition.get());
                            var po = positionRepo.findById(headPosition.get().getEmpPosition());
                            po.ifPresent(obj -> approver.setPositionForwardedToId(po.get()));
                        }
                    }
                }

                Long toPosId = leaveAppDto.getToPosition();
                Optional<Position> toPosDb = positionRepo.findById(toPosId);
                if (toPosDb.isPresent()) {
                    baseLeaveAbroadApp.setToPosition(toPosDb.get());
                }

                baseLeaveAbroadApp.setApproved(approver);
                baseLeaveAbroadApp.setAuditor(getAuditor(userId));
                baseLeaveAbroadApp.setEmpCode(leaveAppDto.getHrEmpCode());
                var religionList = leaveAppRepo.findReligion(leaveAppDto.getHrEmpId());
                if (religionList.size() > 0) {
                    baseLeaveAbroadApp.setReligionId(religionList.get(0).get(0));
                }
                baseLeaveAbroadApp.setAuditor(getAuditor(userId));
                baseLeaveAbroadApp = baseLeaveAbroadAppRepo.save(baseLeaveAbroadApp);

                var ofvisEntity = ofvisAppRepo.findById(Long.parseLong(leaveAppDto.getOfvisCode()));
                if (ofvisEntity.isPresent()) {
                    var ofvis = ofvisEntity.get();
                    ofvis.setActive(false);
                    ofvisAppRepo.saveAndFlush(ofvis);
                }

                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.CREATED,
                                ResponseMessage.SUCCESSFULLY_SAVED,
                                baseLeaveAbroadApp.getId()),
                        HttpStatus.OK);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> SendEl ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private ResponseEntity<ResponseDto> SendOther(LeaveAppDto leaveAppDto, Long userId) throws CommonException {
        LeaveOthersApp leaveOthersApp = new LeaveOthersApp();
        Long leaveType = leaveAppDto.getLeaveType();

        try {

            Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);
            List<LeaveOthersApp> alreadyExistOtherList =
                    othersAppRepo.checkIfExistForOtherHrEmpId(leaveAppDto.getHrEmpId(), leaveAppDto.getFromDate(), leaveAppDto.getToDate());
            if (leaveAppDto.getId() == null && !alreadyExistOtherList.isEmpty()) {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.ALREADY_EXIST,
                                alreadyExistOtherList.get(0).getId()),
                        HttpStatus.BAD_REQUEST);
            } else {
                int noOfDaysCLBetween = (int) (DAYS.between(leaveAppDto.getFromDate(), leaveAppDto.getToDate()) + 1);
                leaveOthersApp.setLeaveType(toLevTyDb.get());
                BeanUtils.copyProperties(leaveAppDto, leaveOthersApp);
                Approved approver = new Approved();
                BeanUtils.copyProperties(leaveAppDto, approver);

                leaveOthersApp.setAddressType(AddressType.valueOf(leaveAppDto.getAddressType()));
                Long fromPosId;
                var position = leaveAppRepo.findPositions(leaveAppDto.getHrEmpId());
                if (position.size() > 0) {
                    fromPosId = position.get(0).get(0);
                    Optional<Position> fromPosDb = positionRepo.findById(fromPosId);
                    if (fromPosDb.isPresent()) {
                        var unitId = unitRepo.findUnitWiseLevel(fromPosDb.get().getId(), "LEVEL_2");
                        unitRepo.findById(unitId).ifPresent(leaveOthersApp::setLevelTwoUnit);

                        leaveOthersApp.setPosition(fromPosDb.get());
                        var unitHeadPosition = unitRepo.unitHeadId(fromPosDb.get().getUnit().getId());
                        if (unitHeadPosition.isPresent()) {
                            var headPosition = baseLeaveAppRepo.findPositions(unitHeadPosition.get());
                            var po = positionRepo.findById(headPosition.get().getEmpPosition());
                            po.ifPresent(obj -> approver.setPositionForwardedToId(po.get()));
                        }
                    }
                }

                leaveOthersApp.setTotalDays(noOfDaysCLBetween);

                Long toPosId = leaveAppDto.getToPosition();
                Optional<Position> toPosDb = positionRepo.findById(toPosId);

                if (toPosDb.isPresent()) {
                    leaveOthersApp.setToPosition(toPosDb.get());
                }

                leaveOthersApp.setApproved(approver);
                leaveOthersApp.setAuditor(getAuditor(userId));
                leaveOthersApp.setEmpCode(leaveAppDto.getHrEmpCode());
                var religionList = leaveAppRepo.findReligion(leaveAppDto.getHrEmpId());
                if (religionList.size() > 0) {
                    leaveOthersApp.setReligionId(religionList.get(0).get(0));
                }
                leaveOthersApp.setAuditor(getAuditor(userId));
                leaveOthersApp = othersAppRepo.save(leaveOthersApp);
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.CREATED,
                                ResponseMessage.SUCCESSFULLY_SAVED,
                                leaveOthersApp.getId()),
                        HttpStatus.OK);
            }

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> SendOther ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity save(LeaveAppDto leaveAppDto, Long userId) throws CommonException {
        try {

            String msg = "";
            boolean isLeaveEligible = false;

            Long leaveType = leaveAppDto.getLeaveType();
            Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);

            var empList = baseLeaveAppRepo.isEmployeeLeaveExist(leaveAppDto.getHrEmpId());
            if (empList.isEmpty()) {

                var positionEntity = baseLeaveAppRepo.findPositions(leaveAppDto.getHrEmpId());
                if (positionEntity.isPresent()) {
                    var position = positionRepo.findById(positionEntity.get().getEmpPosition());
                    if (position.isPresent()) {
                        if (!leaveAppDto.isEmergency()) {
                            var positionObj = position.get();
                            var unit = positionObj.getUnit();
                            var leaveTypeObj = toLevTyDb.get().getCode();

                            LocalDate fromDate;
                            LocalDate toDate;
                            if (leaveTypeObj.equals("CL_PL")) {
                                if (DAYS.between(leaveAppDto.getFromDate(), leaveAppDto.getPlFromDate()) > 0) {
                                    fromDate = leaveAppDto.getFromDate();
                                    toDate = leaveAppDto.getToDate();
                                } else {
                                    fromDate = leaveAppDto.getPlFromDate();
                                    toDate = leaveAppDto.getPlToDate();
                                }
                            } else {
                                fromDate = leaveAppDto.getFromDate();
                                toDate = leaveAppDto.getToDate();
                            }

                            leaveAppRepo.spCallProcedure(unit.getId(), fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                            var dataList = leaveAppRepo.spCall();
                            leaveAppRepo.deleteData();
                            var isFull = dataList.stream().anyMatch(x -> x.getStatus().equals("FULL"));
                            if (isFull) {
                                return new ResponseEntity<>(
                                        new ResponseDto<>(
                                                ResponseStatus.FAILED,
                                                ResponseMessage.NOT_FOUND,
                                                "Sorry! Employee unit quota full. Cannot apply for leave."),
                                        HttpStatus.OK);
                            }
                        }

                        var balanceEntity = balanceSummeryRepo.findAllLeaveBalanceByTypeId(leaveAppDto.getHrEmpId(), leaveAppDto.getLeaveType());
                        if (balanceEntity.isPresent()) {
                            var balance = balanceEntity.get();
                            switch (balance.getLeaveType().getCode()) {
                                case "LWP":
                                    isLeaveEligible = true;
                                    break;
                                case "LHAP":
                                    isLeaveEligible = calculateLhapLeave(balance, leaveAppDto);
                                    break;
                                case "CL_PL":
                                    isLeaveEligible = calculateClPlLeave(balance, leaveAppDto);
                                    break;
                                case "PRL":
                                    isLeaveEligible = true;
                                    break;
                                case "LAP":
                                    isLeaveEligible = calculateLapLeave(balance, leaveAppDto);
                                    break;
                                case "RRL":
                                    isLeaveEligible = (int) Math.abs(DAYS.between(balance.getOnDate(), LocalDate.now())) >= 0;
                                    break;
                                case "CL":
                                    isLeaveEligible = calculateClLeave(balance, leaveAppDto.getFromDate(), leaveAppDto.getToDate());
                                    break;
                                case "PL":
                                    isLeaveEligible = calculatePlLeave(leaveAppDto.getFromDate(), leaveAppDto.getToDate());
                                    break;
                                case "MTL":
                                    isLeaveEligible = maternityLeave(balance);
                                    break;
                                case "ML":
                                    isLeaveEligible = true;
                                    break;
                                case "EL":
                                    isLeaveEligible = calculateClLeave(balance, leaveAppDto.getFromDate(), leaveAppDto.getToDate());
                                    break;
                            }
                            msg = "Employee leave not available";
                        } else {
                            isLeaveEligible = true;
                        }

                        if (!isLeaveEligible) {
                            msg = "Employee leave not available";
                        } else {
                            return getResponseDtoResponseEntity(leaveAppDto, userId, toLevTyDb);
                        }
                    } else {
                        msg = "Employee position not found";
                    }
                } else {
                    msg = "Employee leave not available";
                }


            }
            else if (toLevTyDb.get().getCode().equals("EL")&&!checkOverseaseLeaveAvailibity(leaveAppDto, userId, toLevTyDb)) {

                    return getResponseDtoResponseEntity(leaveAppDto, userId, toLevTyDb);
            }
            else
                msg = "Employee can't apply for another leave";





            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.FAILED,
                            ResponseMessage.ALREADY_EXIST,
                            msg),
                    HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> save ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private boolean checkOverseaseLeaveAvailibity(LeaveAppDto leaveAppDto, Long userId, Optional<LeaveType> toLevTyDb) {

//        log.info("userId:"+userId + "status:"+baseLeaveAppRepo.isEmployeeOverseaseLeaveExist(userId));
       if(baseLeaveAppRepo.isEmployeeOverseaseLeaveExist(userId)){
          return true;

        }
       else
           return false;

    }


    private boolean calculateClPlLeave(LeaveBalanceSummery balance, LeaveAppDto leaveAppDto) {
        if (calculateClLeave(balance, leaveAppDto.getFromDate(), leaveAppDto.getToDate())) {
            return calculatePlLeave(leaveAppDto.getPlFromDate(), leaveAppDto.getPlToDate());
        }
        return false;
    }

    private boolean calculatePlLeave(LocalDate fromDate, LocalDate toDate) {
        var calendar = calendarRepo.findAllCalendar(fromDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        if (calendar.isPresent()) {
            var totalCount = calendar.get();
            var applyedRange = DAYS.between(fromDate, toDate);
            return totalCount == applyedRange;
        }
        return false;
    }

    private boolean calculateLhapLeave(LeaveBalanceSummery balance, LeaveAppDto appDto) {
        var joinDate = balanceSummeryRepo.findHrEmpJoinDate(appDto.getHrEmpId());
        if (joinDate.isPresent()) {
            var dateRange = configurationRepo.findByLeaveType(LHAP);
            var duration = (DAYS.between(joinDate.get().getJod(), LocalDate.now()));
            var length = (int) (DAYS.between(appDto.getFromDate(), appDto.getToDate())) + 1;
            if (dateRange.isPresent()) {
                var total = (duration / Integer.parseInt(dateRange.get().getValue()));
                var spend = balance.getQty();
                return (total - spend) > length;
            } else {
                var total = (duration / 12);
                var spend = 0;
                return (total - spend) > length;
            }
        }
        return false;
    }

    private boolean calculateLapLeave(LeaveBalanceSummery balance, LeaveAppDto appDto) {
        var joinDate = balanceSummeryRepo.findHrEmpJoinDate(appDto.getHrEmpId());
        if (joinDate.isPresent()) {
            var dateRange = configurationRepo.findByLeaveType(LAP);
            var duration = (DAYS.between(joinDate.get().getJod(), LocalDate.now()));
            var length = (int) (DAYS.between(appDto.getFromDate(), appDto.getToDate())) + 1;
            if (dateRange.isPresent()) {
                var total = (duration / Integer.parseInt(dateRange.get().getValue()));
                var spend = balance.getQty();
                return (total - spend) > length;
            } else {
                var total = (duration / 11);
                var spend = 0;
                return (total - spend) > length;
            }
        }
        return false;
    }

    private boolean maternityLeave(LeaveBalanceSummery balance) {
        var config = configurationRepo.findByLeaveType(GlobalConfigType.MTL);
        if (config.isPresent()) {
            var val = config.get().getValue();
            return balance.getQty() < Integer.parseInt(val);
        }
        return balance.getQty() < 2;
    }

    private boolean calculateClLeave(LeaveBalanceSummery summery, LocalDate fromDate, LocalDate toDate) {
        var clConfig = configurationRepo.findByLeaveType(GlobalConfigType.CL);
        var duration = (int) DAYS.between(fromDate, toDate) + 1;
        if (clConfig.isPresent()) {
            var cl = Integer.parseInt(clConfig.get().getValue());
            return (summery.getQty() + duration) <= cl;
        } else {
            return (summery.getQty() + duration) <= 30;
        }
    }

    private ResponseEntity<ResponseDto> getResponseDtoResponseEntity(LeaveAppDto leaveAppDto, Long userId, Optional<LeaveType> toLevTyDb) throws CommonException {
        try{
        if (toLevTyDb.isPresent()) {
            switch (toLevTyDb.get().getLeaveCat()) {
                case REGULAR:
                    return sendClAndPL(leaveAppDto, userId); // LEAVE CAT REGULAR
                case CL_PL:
                    return sendClPl(leaveAppDto, userId); // LEAVE CAT CLPL
                case ABROAD:
                    return SendEl(leaveAppDto, userId); // LEAVE CAT X_BD
                case OTHERS:
                    return SendOther(leaveAppDto, userId); // LEAVE CAT OTHERS
            }
        }

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.FAILED,
                        ResponseMessage.ALREADY_EXIST,
                        null),
                HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> save ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }


    private Auditor updateAuditor(Auditor currentAuditor, Long userId) {
        Auditor auditor = new Auditor();
        auditor.setInsertBy(currentAuditor.getInsertBy());
        auditor.setInsertDate(currentAuditor.getInsertDate());
        auditor.setUpdateBy(userId);
        auditor.setUpdateDate(LocalDateTime.now());
        return auditor;
    }

    public ResponseEntity<ResponseDto> update(LeaveAppDto leaveAppDto, Long userId) throws CommonException {
        try {
            Long leaveType = leaveAppDto.getLeaveType();
            Optional<LeaveType> toLevTyDb = leaveTypeRepo.findById(leaveType);
            return getResponseDtoResponseEntity(leaveAppDto, userId, toLevTyDb);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> update ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> getLeaveBalance(Long userId) {

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.UPDATED,
                        ResponseMessage.SUCCESSFULLY_UPDATED,
                        balanceService.getLeaveBalance(userId)),
                HttpStatus.OK);
    }

    public ResponseEntity<ResponseDto> getLeaveSpendingPlace() {

        var addressType = Arrays.stream(AddressType.values()).map(obj -> {
            switch (obj.name()) {
                case "PRESENT":
                    return new EnumDropdown(obj.name(), "Present Address(বর্তমান ঠিকানা)", "বর্তমান ঠিকানা");
                case "PERMANENT":
                    return new EnumDropdown(obj.name(), "Permanent Address(স্থায়ী ঠিকানা)", "স্থায়ী ঠিকানা");
                case "SPOUSE":
                    return new EnumDropdown(obj.name(), "Spouse Address(স্পাউস ঠিকানা)", "স্পাউস ঠিকানা");
                case "OTHERS":
                    return new EnumDropdown(obj.name(), "Other Address(অন্যান্য)", "অন্যান্য");
                default:
                    return new EnumDropdown();
            }
        }).collect(Collectors.toList());

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        addressType),
                HttpStatus.OK);
    }

    public ResponseEntity<ResponseDto> getLeaveCategories() {
        var leaveTypes = Arrays.stream(LeaveCat.values()).map(obj -> {
            switch (obj.name()) {
                case "REGULAR":
                    return new EnumDropdown(obj.name(), "Regular Leave", "বর্তমান ঠিকানা");
                case "CL_PL":
                    return new EnumDropdown(obj.name(), "CL With PL Leave", "স্থায়ী ঠিকানা");
                case "ABROAD":
                    return new EnumDropdown(obj.name(), "X-Bangladesh Leave", "স্পাউস ঠিকানা");
                case "OTHERS":
                    return new EnumDropdown(obj.name(), "Other Leave", "অন্যান্য");
                default:
                    return new EnumDropdown();
            }
        }).collect(Collectors.toList());

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                        leaveTypes),
                HttpStatus.OK);
    }

    public ResponseEntity<ResponseDto> getAllApprovalStatus() throws CommonException {
        try {
            List<LeaveAppGetAllApprovalStatusResDto> approvalStatusRes = new ArrayList<>();

            for (ApprovalStatus statusType : ApprovalStatus.values()) {
                LeaveAppGetAllApprovalStatusResDto resObj = new LeaveAppGetAllApprovalStatusResDto();
                resObj.setId(statusType.name());
                resObj.setName(statusType.name().replace("_", " "));
                approvalStatusRes.add(resObj);
            }

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                            approvalStatusRes),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getAllApprovalStatus ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> getAllDocType() throws CommonException {
        try {
            List<DropdownEnumResDto> docTypeRes = new ArrayList<>();

            for (DocType docType : DocType.values()) {
                DropdownEnumResDto resObj = new DropdownEnumResDto();
                resObj.setId(docType.name());
                resObj.setName(docType.name().replace("_", " "));
                docTypeRes.add(resObj);
            }

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                            docTypeRes),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getAllDocType ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /*****
     * @Author Danial Chakma
     * Get positions of one layer up of current logged in user
     * @param authUserId
     * @return
     * @throws CommonException
     */
    public ResponseEntity<ResponseDto> getPositions(Long authUserId) throws CommonException {

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            UnitResDto unitRes = webClient
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.GET_UNIT_ID_BY_USER_ID)
                                    .queryParam("userId", authUserId)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(UnitResDto.class)
                    .block();

            if (unitRes != null && unitRes.getPayload() != null) {
                UnitResSubDto payload = unitRes.getPayload();
                if (payload.getUnitIds().size() > 0) {
                    UnitIdListResDto res = webClient
                            .get()
                            .uri(uriBuilder ->
                                    uriBuilder.path(ExternalApiSegments.GET_PARENT_UNIT_BY_UNIT_IDS)
                                            .queryParam("unitIds", payload.getUnitIds())
                                            .build()
                            )
                            .retrieve()
                            .bodyToMono(UnitIdListResDto.class)
                            .block();
                    List<Long> upUnitIds = res.getPayload();

                    PosListResDto posRemoteRes = webClient
                            .get()
                            .uri(uriBuilder ->
                                    uriBuilder.path(ExternalApiSegments.GET_POSITION_BY_UNIT_IDS)
                                            .queryParam("unitIds", upUnitIds)
                                            .build()
                            )
                            .retrieve()
                            .bodyToMono(PosListResDto.class)
                            .block();
                    List<PosResDto> finalRes = new ArrayList<>();
                    if (posRemoteRes != null) {
                        finalRes = posRemoteRes.getPayload();
                    }

                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.OK,
                                    ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                    finalRes),
                            HttpStatus.OK);

                } else {
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.NOT_FOUND,
                                    ResponseMessage.NOT_FOUND,
                                    "No Auth User's Unit Found!"),
                            HttpStatus.OK);
                }
            }

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.FAILED,
                            ResponseMessage.NOT_FOUND,
                            null),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getPositions ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> getLeaveDoc(Long userId, DocSearchReqDto docSearchReqDto) throws CommonException {

        /***
         * TODO: filter result by userId
         */

        try {

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            List<LeaveApp> appList = leaveAppRepo.getLeaveDoc(
                    docSearchReqDto.getEmpCode(),
                    docSearchReqDto.getLeaveType(),
                    docSearchReqDto.getRankId(),
                    docSearchReqDto.getReligionId(),
                    docSearchReqDto.getApprovalStatus(),
                    docSearchReqDto.getAppDateFrom(),
                    docSearchReqDto.getAppDateTo(),
                    docSearchReqDto.getLastConsumedLeaveDateFrom(),
                    docSearchReqDto.getLastConsumedLeaveDateTo()
            );

            List<Long> empIdList = appList.stream().map(obj -> obj.getHrEmpId()).collect(Collectors.toList());

            EmpGenInfoResDto genInfoRes = null;

            if (empIdList != null && empIdList.size() > 0) {
                genInfoRes = webClient
                        .get()
                        .uri(uriBuilder ->
                                uriBuilder.path(ExternalApiSegments.GET_GEN_INFO_FOR_APPROVAL)
                                        .queryParam("empIdList", empIdList)
                                        .build()
                        )
                        .retrieve()
                        .bodyToMono(EmpGenInfoResDto.class)
                        .block();
            }

            List<DocResDto> docResDtoList = new ArrayList<>();
            if (genInfoRes != null && genInfoRes.getPayload() != null
                    && genInfoRes.getPayload().size() > 0) {
                docResDtoList = genInfoRes.getPayload();
            }

            List<DocResDto> finalDocResDtoList = docResDtoList;

            List<LeaveDocResDto> finalResDto = appList.stream().map(obj -> {
                LeaveDocResDto docResDto = new LeaveDocResDto();
                docResDto.setId(obj.getId());
                docResDto.setDocType(docSearchReqDto.getDocType());
                Optional<DocResDto> docDtoOpt = finalDocResDtoList.stream().filter(docObj -> docObj.getId().equals(obj.getHrEmpId())).findFirst();
                if (docDtoOpt.isPresent()) {
                    docResDto.setGeneralInfo(docDtoOpt.get().getGeneralInfo());
                }

                // generalInfoList.add(obj.get);
                docResDto.setAppDate(obj.getApproved().getAppDate());
                docResDto.setFromDate(obj.getFromDate());
                docResDto.setToDate(obj.getToDate());
                docResDto.setCountry("");
                docResDto.setLeaveType(obj.getLeaveType() == null ? obj.getLeaveType().getName() : "");

                docResDto.setApprovalStatus(obj.getApproved().getApprovalStatus().name());
                docResDto.setRemarks("");

                return docResDto;
            }).collect(Collectors.toList());

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                            finalResDto),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getLeaveDoc ", ex);
            log.info("Error info:{}", ex.getMessage());
            ex.printStackTrace();
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /****
     *
     * @param userId
     * @param reqDto
     * @return
     * @throws CommonException
     */

    public ResponseEntity<ResponseDto> updateDocumentStatus(Long userId, DocApprovalReqDto reqDto, String token) throws CommonException {

        /****
         * TO DO: insert into the approval history table
         */

        try {

            // get-pos-by-userid-and-units
            UnitResDto unitRes = WebClientConfig.getWebClient(userId, token)
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.GET_UNIT_ID_BY_USER_ID)
                                    .queryParam("userId", userId)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(UnitResDto.class)
                    .block();

            List<Long> unitIdList = null;
            if (unitRes != null && unitRes.getPayload() != null && unitRes.getPayload().getUnitIds().size() > 0) {
                unitIdList = unitRes.getPayload().getUnitIds();
            }

            List<Long> finalUnitIdList = unitIdList;

            AuthUserPosResDto posRes = WebClientConfig.getWebClient(userId, token)
                    .get()
                    .uri(uriBuilder ->
                            uriBuilder.path(ExternalApiSegments.GET_POSITION_BY_UNIT_IDS)
                                    .queryParam("userId", userId)
                                    .queryParam("unitIdList", finalUnitIdList)
                                    .build()
                    )
                    .retrieve()
                    .bodyToMono(AuthUserPosResDto.class)
                    .block();
            Long authUserPosId = null;
            if (posRes != null && posRes.getPayload() != null) {
                authUserPosId = posRes.getPayload();
            }

            Optional<Position> authUserPosOpt = positionRepo.findById(authUserPosId);

            Position authUserPosition = null;
            if (authUserPosOpt.isPresent()) {
                authUserPosition = authUserPosOpt.get();
            }

            Position finalAuthUserPosition = authUserPosition;
            if (reqDto.getAction().equalsIgnoreCase("Forward")) {
                List<Long> docIds = reqDto.getDocIdList();
                Optional<Position> positionOpt = positionRepo.findById(reqDto.getPositionId());

                if (positionOpt.isPresent()) {
                    List<LeaveApp> appList = leaveAppRepo.findAllById(docIds).stream().map(obj -> {
                        obj.getApproved().setApprovalStatus(ApprovalStatus.FORWARDED);
                        obj.setPosition(finalAuthUserPosition);
                        obj.setToPosition(positionOpt.get());
                        return obj;
                    }).collect(Collectors.toList());
                    leaveAppRepo.saveAll(appList);
                } else {
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.FAILED,
                                    ResponseMessage.NOT_FOUND,
                                    "Sorry!, Position not found."),
                            HttpStatus.OK);
                }

            } else if (reqDto.getAction().equalsIgnoreCase("Reject")) {
                List<Long> docIds = reqDto.getDocIdList();
                List<LeaveApp> appList = leaveAppRepo.findAllById(docIds).stream().map(obj -> {
                    obj.getApproved().setApprovalStatus(ApprovalStatus.REJECTED);
                    obj.getApproved().setRemarks(reqDto.getRemarks());
                    //obj.setToPosition(positionOpt.get());
                    obj.setPosition(finalAuthUserPosition);
                    return obj;
                }).collect(Collectors.toList());
                leaveAppRepo.saveAll(appList);
            } else if (reqDto.getAction().equalsIgnoreCase("Approve")) {
                List<Long> docIds = reqDto.getDocIdList();
                List<LeaveApp> appList = leaveAppRepo.findAllById(docIds).stream().map(obj -> {
                    obj.getApproved().setApprovalStatus(ApprovalStatus.APPROVED);
                    obj.getApproved().setRemarks(reqDto.getRemarks());
                    //obj.setToPosition(null);
                    obj.setPosition(finalAuthUserPosition);
                    return obj;
                }).collect(Collectors.toList());
                leaveAppRepo.saveAll(appList);
            } else {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.FAILED,
                                ResponseMessage.NOT_FOUND,
                                "Sorry!, Un-authorized Action."),
                        HttpStatus.OK);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> updateDocumentStatus ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.FAILED,
                        ResponseMessage.NOT_FOUND,
                        null),
                HttpStatus.METHOD_FAILURE);

    }

    /* #################### LEAVE APPLICATION LIST :: START #################### */

    /**
     * get hrEmpId by unitId from hr_module
     *
     * @param unitIdList
     * @return
     */



    private List<HrEmpGetEmpIdByUnitIdDto> getHrEmpIdByUnitId(List<Long> unitIdList, String token) throws CommonException{




        try{
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", token)
                .build();

        var res = Objects.requireNonNull(webClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path(ExternalApiSegments.GET_HR_EMP_ID_BY_UNIT_ID)
                                .queryParam("unitIdList", unitIdList)
                                .build()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebClientCommonResponse<List<HrEmpGetEmpIdByUnitIdDto>>>() {
                }).block()).getData();

        return res;
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getHrEmpIdByUnitId ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * get hr_emp_id by auth_user_id from hr_module
     *
     * @param authUserId
     * @return
     */
    private Long getHrEmpIdByAuthUserId(Long authUserId, String token) throws CommonException {

        try{
        return Objects.requireNonNull(WebClientConfig.getWebClient(authUserId, token)
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path(ExternalApiSegments.GET_HR_EMP_ID_BY_AUTH_USER_ID)
                                .queryParam("authUserId", authUserId)
                                .build()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<WebClientCommonResponse<Long>>() {
                }).block()).getData();
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getHrEmpIdByAuthUserId ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /**
     * get all leave application
     *
     * @param reqDto
     * @param userId
     * @return
     * @throws CommonException
     */
    @Transactional
    public ResponseEntity<ResponseDto> getAllLeaveApp(LeaveSearchCommonReqDto reqDto, Long userId, String token) throws CommonException {
        try {
            List<Long> hrEmpIdList = new ArrayList<>();

            // get auth_user_role by auth_user_id
            Optional<String> resOpt = leaveAppRepo.findAuthUserRoleById(userId);

            if (resOpt.isPresent()) {
                // ROLE RO user
                if (resOpt.get().equals("RO")) {
                    List<Long> unitIdList = leaveAppRepo.findAuthUserUnitById(userId);

                    List<Long> ids = getHrEmpIdByUnitId(unitIdList, token).stream().
                            map(HrEmpGetEmpIdByUnitIdDto::getId).collect(Collectors.toList());

                    hrEmpIdList.addAll(ids);
                }
            }
            // Non ROLE RO user
            else {
                Long hrEmpId = getHrEmpIdByAuthUserId(userId, token);
                hrEmpIdList.add(hrEmpId);
            }

            // validate leave_app_date
            // 1. if fromDate is bot null but toDate is null then current date will be the to date
            // 2. if toDate is not null then fromDate can not be null
            if (reqDto.getAppDateFrom() != null) {
                if (reqDto.getAppDateTo() == null) {
                    reqDto.setAppDateTo(LocalDate.now());
                }
            }

            // validate last_consumed_leave_date
            // 1. if fromDate is bot null but toDate is null then current date will be the to date
            // 2. if toDate is not null then fromDate can not be null
            if (reqDto.getLastConsumedLeaveDateFrom() != null) {
                if (reqDto.getLastConsumedLeaveDateTo() == null) {
                    reqDto.setLastConsumedLeaveDateTo(LocalDate.now());
                }
            }

            Pageable pageRequest = PageRequest.of(reqDto.getPage(), reqDto.getLimit());
            // get leave app list from base_leave_app
            Page<LeaveAppListResDto> res = blRepo.findAllLeaveApp(
                    reqDto.getAppDateFrom(),
                    reqDto.getAppDateTo(),
                    reqDto.getLastConsumedLeaveDateFrom(),
                    reqDto.getLastConsumedLeaveDateTo(),
                    reqDto.getReligionId(),
                    reqDto.getLeaveTypeId(),
                    reqDto.getRankId(),
                    reqDto.getUnitId(),
                    reqDto.getEmpCode(),
                    reqDto.getApprovalStatus(),
                    hrEmpIdList,
                    pageRequest);


            // get hr_emp_id list from full object
            List<Long> empIdList = res.stream().map(LeaveAppListResDto::getHrEmpId).distinct().collect(Collectors.toList());
            // get emp detail from hr_module by hr_emp_id

            List<HrEmpGetAllByIdResDto> empDetail = EmployeesGenInfo.getHrEmpDetailById(empIdList, userId, token);

            List<LeaveAppListFinalResDto> finalRes = res.stream().map(obj -> {

                var details = empDetail.stream()
                        .filter(x -> Objects.equals(x.getId(), obj.getHrEmpId()))
                        .findFirst();

                LeaveAppListFinalResDto resDto = new LeaveAppListFinalResDto();

                resDto.setId(obj.getId());

                String hrEmpGenInfo = "";

                if (details.isPresent()) {
                    var de = details.get();
                    hrEmpGenInfo += de.getCode() + ", " +
                            de.getName() + ", " +
                            de.getDesg() + ", " +
                            de.getReligion() + ", " +
                            de.getOwnDistrict();
                }

                resDto.setHrEmpGenInfo(hrEmpGenInfo);

                String leaveCountry = "";
                if (obj.getCountry() != null && !obj.getCountry().equals("")) {
                    leaveCountry = ", Country: " + obj.getCountry();
                }

                LocalDate fromDate;
                LocalDate toDate;

                if (obj.getLeaveCat().equals("CL_PL")) {
                    if (DAYS.between(obj.getFromDate(), obj.getPlFromDate()) > 0) {
                        fromDate = obj.getFromDate();
                        toDate = obj.getPlToDate();
                    } else {
                        fromDate = obj.getPlFromDate();
                        toDate = obj.getToDate();
                    }
                } else {
                    fromDate = obj.getFromDate();
                    toDate = obj.getToDate();
                }

                String description = ("Type: " + obj.getLeaveTypeName() + leaveCountry +
                        ", Last Leave: " + (obj.getLastConsumedLeaveDate() != null ? obj.getLastConsumedLeaveDate() : "N/A") +
                        ", Application Date: " + obj.getAppDate() +
                        ", Leave Start: " + fromDate +
                        ", Leave End: " + toDate);

                resDto.setDescription(description);
                resDto.setLeaveCat(obj.getLeaveCat());
                resDto.setLap(obj.getLap());
                resDto.setRemarks(obj.getRemarks());

                String statusDetails = "";
                if(Objects.equals(obj.getApprovalStatus(), "OFFICE_ORDERED")) {
                    String[] parts = obj.getApprovalStatus().split("::");
                    statusDetails = "Leave Status : " + parts[0] + " , " + "Extension Status: " + parts[1] ;
                    resDto.setApprovalStatus(statusDetails);
                } else{
                    statusDetails = "Approval Status : " + obj.getApprovalStatus();

                }


//                resDto.setApprovalStatus(statusDetails);
                resDto.setApprovalStatus(obj.getApprovalStatus());


                if (details.isPresent()) {
                    if (details.get().getRankLevel() != null) {
                        int rankValDB = RankLevel.valueOf(details.get().getRankLevel()).value;
                        int rankValEnum = RankLevel.valueOf("SI").value;

                        if (rankValDB <= rankValEnum) {
                            resDto.setEnableCertificationBtn("SI_TO_UP");
                        } else {
                            resDto.setEnableCertificationBtn("ASI_TO_DOWN");
                        }
                    } else {
                        resDto.setEnableCertificationBtn("SI_TO_UP");
                    }
                }
                return resDto;
            }).collect(Collectors.toList());

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                            new PagedResDto<>(finalRes, reqDto.getPage(), reqDto.getLimit(), res.getTotalElements())),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getAllLeaveApp ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> submitLeaveApp(Long id, Long userId) throws CommonException {

        try {
            var data = leaveAppRepo.findLeaveById(id);
            if (data.isPresent()) {
//                Long maxId = jdbcTemplate.queryForObject("select max(id) from APPROVAL_HISTORY", Long.class);
                Long maxId = jdbcTemplate.queryForObject("select seq_approval_history.nextval from dual", Long.class);


                Map<String, Object> empInfo = jdbcTemplate.queryForMap("SELECT ID, POSITION_ID FROM HR_EMP WHERE AUTH_USER_ID = ? ", userId);

                if (empInfo == null) {
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.FAILED,
                                    ResponseMessage.NOT_FOUND,
                                    "Auth User not connected with employee."),
                            HttpStatus.OK);
                }

                Object hrEmpActionById = empInfo.get("ID");
                Object positionActionById = empInfo.get("POSITION_ID"); // employee position should not be null in table.

                jdbcTemplate.update("INSERT INTO APPROVAL_HISTORY( ID, MODULE, DOC_TYPE, MST_ID," +
                                "ACTION_DATE, APPROVAL_ACTION, AUTH_USER_ACTION_BY_ID," +
                                "HR_EMP_ACTION_BY_ID, POSITION_ACTION_BY_ID, FWD_POSITION_ID," +
                                "REMARKS ) " +
                                "VALUES ( ?, ?, ?,?,SYSDATE,?,?,?,?,?,? )",
                        maxId, "LEAVE", DocType.LEAVE_APP.name(), id,
                        ApprovalAction.SUBMITTED.name(), userId,
                        hrEmpActionById, positionActionById, null,
                        "Submitted"
                );

                leaveAppRepo.updateLeaveApprovalStatus(id, ApprovalStatus.SUBMITTED);
            }
            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.SUCCESSFULLY_UPDATED,
                            true),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during submitting leave application", ex);
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> reSubmitLeaveApp(Long id, Long userId) throws CommonException {

        try {
            var data = leaveAppRepo.findLeaveById(id);
            if (data.isPresent()) {
//                Long maxId = jdbcTemplate.queryForObject("select max(id) from APPROVAL_HISTORY", Long.class);
//                if (maxId == null) {
//                    maxId = 0L;
//                }
                Long maxId = jdbcTemplate.queryForObject("select seq_approval_history.nextval from dual", Long.class);


                Map<String, Object> empInfo = jdbcTemplate.queryForMap("SELECT ID, POSITION_ID FROM HR_EMP WHERE AUTH_USER_ID = ? ", userId);

                if (empInfo == null) {
                    return new ResponseEntity<>(
                            new ResponseDto<>(
                                    ResponseStatus.FAILED,
                                    ResponseMessage.NOT_FOUND,
                                    "Auth User not connected with employee."),
                            HttpStatus.OK);
                }

                Object hrEmpActionById = empInfo.get("ID");
                Object positionActionById = empInfo.get("POSITION_ID"); // employee position should not be null in table.

                jdbcTemplate.update("INSERT INTO APPROVAL_HISTORY( ID, DOC_TYPE, MST_ID," +
                                "ACTION_DATE, APPROVAL_ACTION, AUTH_USER_ACTION_BY_ID," +
                                "HR_EMP_ACTION_BY_ID, POSITION_ACTION_BY_ID, FWD_POSITION_ID," +
                                "REMARKS ) " +
                                "VALUES ( ?,?,?,SYSDATE,?,?,?,?,?,? )",
                        maxId, DocType.LEAVE_APP.name(), id,
                        ApprovalAction.RE_SUBMITTED.name(), userId,
                        hrEmpActionById, positionActionById, null,
                        "Re-Submitted"
                );

                leaveAppRepo.updateLeaveApprovalStatus(id, ApprovalStatus.RE_SUBMITTED);
            }
            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.SUCCESSFULLY_UPDATED,
                            true),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during reSubmitLeaveApp leave application", ex);
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    /* #################### LEAVE APPLICATION LIST :: END #################### */


    public ResponseEntity<ResponseDto> getLeaveDetail(Long mstId) throws CommonException {

        try {
            LeaveAppDetailDto responseDto = leaveAppRepo.getLeaveDocById(mstId);
            if (responseDto != null) {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.OK,
                                ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                responseDto),
                        HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.NOT_FOUND,
                                ResponseMessage.NOT_FOUND,
                                null),
                        HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getLeaveDetail ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> getLeaveEmpInfo(Long id) throws CommonException {

        try {
//            Optional<LeaveEmpInfoDto> empInfo;
            var empInfo = baseLeaveAppRepo.findLeaveEmpInfo(id);
//          Optional<BaseLeaveApp>  baseLeaveApp =baseLeaveAppRepo.findById(id);
            LeaveEmpInfoDto empInfoDto = new LeaveEmpInfoDto();


            if (empInfo != null) {
//                BeanUtils.copyProperties(empInfoDto, empInfo);
                empInfoDto.setCode(empInfo.get().getCode());
                empInfoDto.setEmpName(empInfo.get().getNameNative());
                empInfoDto.setFromDate(empInfo.get().getFromDate());
                empInfoDto.setToDate(empInfo.get().getToDate());
                empInfoDto.setAppDate(empInfo.get().getAppDate());
                empInfoDto.setApprovalStatus(empInfo.get().getApprovalStatus());
                empInfoDto.setLeaveType(empInfo.get().getName());
//                empInfoDto.setAppDate(empInfo.get().getAppDate());
//                empInfoDto.setEmpName(baseLeaveApp.get().getHrEmpId());
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.OK,
                                ResponseMessage.ENTITY_RETRIEVED_SUCCESS,
                                empInfoDto),
                        HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                        new ResponseDto<>(
                                ResponseStatus.NOT_FOUND,
                                ResponseMessage.NOT_FOUND,
                                null),
                        HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getLeaveEmpInfo ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> onLeaveCancel(Long id, Long userId) throws CommonException {

        try {
            var data = leaveAppRepo.findLeaveById(id);
            if (data.isPresent()) {
                leaveAppRepo.updateLeaveStatus(id, LeaveStatus.ON_CANCEL);
            }
            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.SUCCESSFULLY_UPDATED,
                            true),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> onLeaveCancel ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> onLeaveDelete(Long id) throws CommonException {
        try {
            leaveAppRepo.deleteLeave(id);
            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.SUCCESSFULLY_DELETED,
                            true),
                    HttpStatus.OK);

        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> onLeaveDelete ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public ResponseEntity<ResponseDto> onAvailableLeaveCheck(LeaveCheck leaveCheck) throws CommonException{
        try{
        var leave = baseLeaveAppRepo.checkLeaveAvailable(
                leaveCheck.getFromDate(),
                leaveCheck.getToDate(),
                leaveCheck.getUnitId(),
                leaveCheck.getRestrict(),
                leaveCheck.getReserved());

        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.OK,
                        ResponseMessage.SUCCESSFULLY_DELETED,
                        leave == 1),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> onAvailableLeaveCheck ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }

    }

    public ResponseEntity<ResponseDto> getCalender(Long userId, Long hrEmpId) throws CommonException {
        try{
        var empPo = baseLeaveAppRepo.findPositions(hrEmpId);
        var tt=empPo.get().getEmpPosition();

        Long unitId =  jdbcTemplate.queryForObject("Select U.ID from POSITION P JOIN UNIT U ON P.UNIT_ID = U.ID  WHERE P.ID =   " + tt   , Long.class  );



       // var po = positionRepo.findById(empPo.get().getEmpPosition());
       // po = positionRepo.findById(22419L);
//        var po = positionRepo.findPositionById(empPo.get().getEmpPosition());

        if (unitId != null) {
//            var position = po.get();
//            var unit = position.getUnit();
            leaveAppRepo.spCallProcedure(unitId, "", "");
            var dataList = leaveAppRepo.spCall();
            leaveAppRepo.deleteData();

            return new ResponseEntity<>(
                    new ResponseDto<>(
                            ResponseStatus.OK,
                            ResponseMessage.SUCCESSFULLY_DELETED,
                            dataList),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(
                new ResponseDto<>(
                        ResponseStatus.NOT_FOUND,
                        ResponseMessage.NOT_FOUND,
                        "Sorry! Position Not Found"),
                HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Exception occurred during execution of -> getCalender ", ex);
            log.info("Error info:{}", ex.getMessage());
            throw new CommonException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
}