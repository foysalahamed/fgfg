


import com.ibcs.ngpims.dto.EmpLastOneYearAbrdLeaveReportDto;
import com.ibcs.ngpims.dto.LeaveAppDetailDto;
import com.ibcs.ngpims.dto.leave.ICalenderDto;
import com.ibcs.ngpims.dto.leave.IHrEmpBasicInfo;
import com.ibcs.ngpims.dto.leave.IHrEmpLeaveTotal;
import com.ibcs.ngpims.dto.leave.IHrEmpPosition;
import com.ibcs.ngpims.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveAppRepo extends JpaRepository<LeaveApp, Long>, JpaSpecificationExecutor<LeaveApp> {


    @Query(value = "select leave from LeaveApp leave where 1=1 " +
            "and leave.officeOrder.id = :officeOrderId ")
    List<LeaveApp> findAllOfficeOrderById(@Param("officeOrderId") Long officeOrderId);


    @Query(value = "select B.FROM_DATE as FROM_DATE,B.TO_DATE as TO_DATE, b.TOTAL_DAYS as totalDays,HR_EMP.NAME_NATIVE as name," +
            "                 P.NAME_NATIVE as PositionName, UNIT.NAME_NATIVE as unitName,L.NAME as country, O.SRC_OF_FUNDING as srcOfFunding, B.REASON as REASON, B.REMARKS as REMARKS\n" +
            "                from BASE_LEAVE_APP b " +
            "                 LEFT JOIN HR_EMP ON HR_EMP.ID= B.HR_EMP_ID  " +
            "                 LEFT JOIN LEAVE_ABROAD_APP la ON la.ID=b.ID " +
            "                 LEFT JOIN OFVIS_APP o ON la.OFVIS_CODE = o.CODE " +
            "                 LEFT JOIN LOOKUP l ON la.COUNTRY_ID=l.ID " +
            "                 LEFT JOIN POSITION p ON b.POSITION_ID=p.ID" +
            "                 LEFT JOIN UNIT ON p.UNIT_ID=UNIT.ID " +
            "                 WHERE trunc(b.APP_DATE) BETWEEN  TO_DATE(sysdate - 365)  AND TO_DATE(sysdate) and b.HR_EMP_ID=:hrEmpId and b.LEAVE_TYPE_ID=9" +
            "                UNION\n" +
            "select  FTR.FROM_DATE as FROM_DATE, FTR.TO_DATE as TO_DATE, (FTR.TO_DATE-FTR.FROM_DATE) AS totalDays, emp.NAME_NATIVE as name, P.NAME as PositionName," +
            "             UNIT.NAME_NATIVE as unitName, L.NAME as country,FTR.SRC_OF_FUNDING as srcOfFunding, FTR.TOUR_TYPE as REASON , 'previous entry' as REMARKS from HR_EMP_FOREIGN_TOUR ftr " +
            "             LEFT JOIN HR_EMP emp ON emp.ID  = FTR.HR_EMP_ID  " +
            "             LEFT JOIN LOOKUP l ON FTR.COUNTRY_ID =l.ID " +
            "             LEFT JOIN POSITION p ON EMP.POSITION_ID =p.ID" +
            "             LEFT JOIN UNIT ON EMP.UNIT_ID =UNIT.ID " +
            "             WHERE trunc(FTR.FROM_DATE ) BETWEEN  TO_DATE(sysdate - 365)  AND TO_DATE(sysdate) and FTR.HR_EMP_ID =:hrEmpId  ", nativeQuery = true)
    List<EmpLastOneYearAbrdLeaveReportDto> findEmpLastOneYearAbrdLeaveReport(@Param("hrEmpId") Long hrEmpId);

    @Query(value = "select l from BaseLeaveApp l where l.id = :leaveId")
    Optional<BaseLeaveApp> findLeaveById(@Param("leaveId") Long leaveId);

    @Query(value = "SELECT UT.HEAD_OF_UNIT_ID FROM UNIT UT, POSITION PO where UT.ID = PO.UNIT_ID AND PO.ID = :positionId ", nativeQuery = true)
    Optional<Long> findUnitHeadByPositionId(@Param("positionId") Long positionId);


    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.approved.approvalStatus = :approvalStatus where ll.id = :leaveId ")
    void updateLeaveApprovalStatus(@Param("leaveId") Long leaveId, @Param("approvalStatus") ApprovalStatus approvalStatus);

    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.leaveStatus = :leaveStatus where ll.id = :leaveId ")
    void updateLeaveStatus(@Param("leaveId") Long leaveId, @Param("leaveStatus") LeaveStatus leaveStatus);

    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.deleted = true, ll.leaveStatus = 'DONE' where ll.id = :leaveId ")
    void deleteLeave(@Param("leaveId") Long leaveId);

    @Query("select u from LeaveApp u " +
            "where u.hrEmpId = :hrEmpId " +
            "AND ( u.fromDate between :fromDate and :toDate or  u.toDate between :fromDate and :toDate ) ")
    List<LeaveApp> checkIfExistForHrEmpId(@Param("hrEmpId") Long hrEmpId,
                                          @Param("fromDate") LocalDate fromDate,
                                          @Param("toDate") LocalDate toDate);

    @Query(value = " select la from LeaveApp la " +
            " where ( :empCode is null or la.empCode = :empCode) " +
            " and ( :leaveType is null or la.leaveType.id = :leaveType ) " +
            " and ( :religionId is null or la.religionId = :religionId ) " +
            " and ( :rankId is null or la.position.rank.id = :rankId ) " +
            " and (:approvalStatus is null or concat(la.approved.approvalStatus ,'') = :approvalStatus) " +
            " and ( concat(la.approved.approvalStatus,'') not in ('PENDING', 'APPROVED', 'REJECTED') ) " +
            " and (:consumedDateFrom is null or :consumedDateTo is null or ( la.fromDate between :consumedDateFrom and :consumedDateTo and la.toDate between :consumedDateFrom and :consumedDateTo ) ) " +
            " and (:appDateFrom is null or :appDateTo is null or la.approved.appDate between :appDateFrom and :appDateTo ) "
    )
    List<LeaveApp> getLeaveDoc(@Param("empCode") String empCode,
                               @Param("leaveType") Long leaveType,
                               @Param("rankId") Long rankId,
                               @Param("religionId") Long religionId,
                               @Param("approvalStatus") String approvalStatus,
                               @Param("appDateFrom") LocalDate appDateFrom,
                               @Param("appDateTo") LocalDate appDateTo,
                               @Param("consumedDateFrom") LocalDate consumedDateFrom,
                               @Param("consumedDateTo") LocalDate consumedDateTo);

    @Query(value = "select  POSITION_ID from HR_EMP where ID = :empId and POSITION_ID is not null", nativeQuery = true)
    List<List<Long>> findPositions(@Param("empId") Long empId);

    @Query(value = "select RELIGION_ID from HR_EMP where ID = :empId and RELIGION_ID is not null", nativeQuery = true)
    List<List<Long>> findReligion(@Param("empId") Long empId);


    @Query(value = "select lv from LeaveApp lv where 1=1 " +
            "and(:hrEmpId is null or lv.hrEmpId = :hrEmpId) " +
            "and lv.approved.approvalStatus = 'OFFICE_ORDERED' ")
    List<LeaveApp> loadOfficeOrderDepEmployees(
            @Param("hrEmpId") Long hrEmpId);

    @Query(value = "select la.id from LeaveApp la where la.hrEmpId =:hrEmpId and la.approved.approvalStatus = 'OFFICE_ORDERED' ")
    Optional<LeaveApp> getByEmpId(@Param("hrEmpId") Long hrEmpId);

    @Query(value = "select max(bla1.fromDate) from LeaveApp bla1 where bla1.hrEmpId = :hrEmpId and bla1.fromDate is not null and bla1.approved.approvalStatus <> 'PENDING'")
    List<LocalDate> getPreviousLeaveDate(@Param("hrEmpId") Long hrEmpId);

    /* #################### LEAVE APPLICATION LIST :: START #################### */

    @Query(value = "select ar.code " +
            "from UserRole aur " +
            "inner join AuthRole ar on ar.id = aur.authRole.id " +
            "where ar.code = 'RO' and aur.authUser.id = :userId")
    Optional<String> findAuthUserRoleById(@Param("userId") Long userId);

    @Query(value = "select auu.unit.id " +
            "from UserUnit auu " +
            "inner join AuthUser au on au.id = auu.authUser.id " +
            "where auu.authUser.id = :userId")
    List<Long> findAuthUserUnitById(@Param("userId") Long userId);


    @Query(value = " select he.id from HrEmp he join AuthUser au on  he.authUserId = au.id where au.id = :id")
    Long findHrEmpIdbyAuthUserId(@Param("id") Long id);



    /* #################### LEAVE APPLICATION LIST :: END #################### */


    @Query(value = "select base from BaseLeaveApp base where base.id in (:ids)")
    List<BaseLeaveApp> findAllBaseLeaveById(@Param("ids") List<Long> ids);

    @Query(value = " select new com.ibcs.ngpims.dto.LeaveAppDetailDto(la.id, la.leaveType.name, la.approved.appDate, la.fromDate, la.toDate, la.addressType, la.othersAddress, la.reason, la.approved.remarks ) " +
            " from LeaveApp la where la.id = :id ")
    LeaveAppDetailDto getLeaveDocById(@Param("id") Long id);

    @Query(value = "select base from BaseLeaveApp base where base.id = :ids ")
    Optional<BaseLeaveApp> findBaseLeaveById(@Param("ids") Long id);


    @Query(value = "select distinct oo from BaseLeaveApp oo " +
            "left join BaseLeaveClPlApp cl on oo.id = cl.id " +
            "where 1=1 and oo.approved.approvalStatus = 'OFFICE_ORDERED' " +
            "and concat(oo.leaveStatus, '') in ('START', 'ON_LEAVE') " +
            "and concat(oo.dispatchStatus,'') = 'APPROVED' " +
            "and(:leaveType is null or oo.leaveType.id = :leaveType) " +
            "and  oo.hrEmpId in (:hrEmpIdList) " +
            " and (:unitId is null or oo.position.unit.id = :unitId) " +
            " and  coalesce(cl.plFromDate,oo.fromDate) between TO_DATE(:fromDate, 'YYYY-MM-DD' ) and TO_DATE(:toDate, 'YYYY-MM-DD')   " +
            " order by oo.approved.approvedDate desc ")
    List<BaseLeaveApp> findAllOfficeOrdered(
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            @Param("leaveType") Long leaveType,
            @Param("unitId") Long unitId,
            @Param("hrEmpIdList") List<Long> hrEmpIdList
    );

    @Query(value = "select distinct oo from BaseLeaveApp oo " +
            "left join BaseLeaveClPlApp cl on oo.id = cl.id " +
            "left join Position ps on ps.id = oo.position.id " +
            "left join Unit un on un.id = ps.unit.id " +
            "where 1=1 and oo.approved.approvalStatus = 'OFFICE_ORDERED' " +
            "and concat(oo.leaveStatus, '') in ('START', 'ON_LEAVE') " +
            "and concat(oo.dispatchStatus,'') = 'APPROVED' " +
            "and(:leaveType is null or oo.leaveType.id = :leaveType) " +
            "and oo.hrEmpId in (:hrEmpIdList) " +
//            "and (" +
//            "(cl.plFromDate is null and oo.fromDate = TO_DATE(:date, 'YYYY-MM-DD')) or " +
//            "(cl.plFromDate > oo.fromDate and oo.fromDate = TO_DATE(:date, 'YYYY-MM-DD')) or " +
//            "(cl.plFromDate = TO_DATE(:date, 'YYYY-MM-DD'))) " +//
            "and(:unitId is null or oo.position.unit.id = :unitId) " +
            "and(:fromDate is null or  ( cl.plFromDate is null and oo.fromDate between TO_DATE(:fromDate, 'YYYY-MM-DD') and  TO_DATE(:toDate, 'YYYY-MM-DD')) " +
            "or (cl.plFromDate > oo.fromDate and oo.fromDate between TO_DATE(:fromDate, 'YYYY-MM-DD') and  TO_DATE(:toDate, 'YYYY-MM-DD')) or " +
            // may be mistekenly written oo.plFromDate
            "cl.plFromDate between TO_DATE(:fromDate, 'YYYY-MM-DD') and TO_DATE(:toDate, 'YYYY-MM-DD')) " +
            " order by oo.approved.approvedDate desc ")
    List<BaseLeaveApp> findAllOfficeOrderedSearch(
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            @Param("leaveType") Long leaveType,
            @Param("unitId") Long unitId,
            @Param("hrEmpIdList") List<Long> hrEmpIdList
//            @Param("date") String date
    );


    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.leaveStatus = :leaveStatus where ll.id = :lId ")
    void updateBaseLeaveApp(@Param("lId") Long lId, @Param("leaveStatus") LeaveStatus leaveStatus);

    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.joinDate = :joiningDate, ll.leaveExtStatus = :leaveExtStatus  where ll.id = :lId ")
    void updateJoiningDate(@Param("lId") Long lId, @Param("joiningDate") LocalDateTime joiningDate, @Param("leaveExtStatus") LeaveExtStatus extStatus);


    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.joinDate = :joiningDate, ll.leaveExtStatus = :leaveExtStatus  where ll.id = :lId ")
    void updatePastForeignVisit(@Param("lId") Long lId, @Param("joiningDate") LocalDate joiningDate, @Param("leaveExtStatus") LeaveExtStatus extStatus);


    @Query(value = "select ID, POSITION_ID as empPosition from HR_EMP where AUTH_USER_ID = :authId and POSITION_ID is not null", nativeQuery = true)
    Optional<IHrEmpPosition> findAuthUserInfo(@Param("authId") Long authId);


//    LeaveEmpInfo


    @Query(value = "SELECT E.CODE CODE, E.NAME NAME, \n" +
            "                    E.NAME_NATIVE NAMENATIVE, \n" +
            "                    (EXT.PERMANENT_ADDRESS ||'-'||EXT.PERMANENT_VILLAGE_NAME_NATIVE ||'-'|| LV.NAME_NATIVE||'-'||PVD.NAME_NATIVE||'-'||PLC.NAME_NATIVE ) as PERMANENTADDRESS, \n" +
            "                    (EXT.PRESENT_ADDRESS ||'-'||EXT.PRESENT_VILLAGE_NAME_NATIVE ||'-'|| LT.NAME_NATIVE||'-'||PTD.NAME_NATIVE ) as PRESENTADDRESS, \n" +
            "                    (EXT.SPOUSE_ADDRESS||'-'||EXT.SPOUSE_VILLAGE_NAME_NATIVE ||LS.NAME_NATIVE||'-'||PSD.NAME_NATIVE||'-'||PSC.NAME_NATIVE ) SPOUSEADDRESS\n" +
            "                    FROM HR_EMP E \n" +
            "                    LEFT JOIN HR_EMP_EXT EXT ON E.ID = EXT.HR_EMP_ID \n" +
            "                    LEFT JOIN LOC_VALUE LV ON EXT.PERMANENT_THANA_ID =LV.ID\n" +
            "                    LEFT JOIN LOC_VALUE PVD ON EXT.PERMANENT_DIST_ID =PVD.ID\n" +
            "                    LEFT JOIN LOC_VALUE PLC ON EXT.PERMANENT_DIV_ID =PLC.ID\n" +
            "                    LEFT JOIN LOC_VALUE LT ON EXT.PRESENT_THANA_ID =LT.ID\n" +
            "                    LEFT JOIN LOC_VALUE PTD ON EXT.PRESENT_DIST_ID =PTD.ID\n" +
            "                    LEFT JOIN LOC_VALUE PTC ON EXT.PRESENT_DIV_ID =PTC.ID\n" +
            "                    LEFT JOIN LOC_VALUE LS ON EXT.SPOUSE_THANA_ID =LS.ID\n" +
            "                    LEFT JOIN LOC_VALUE PSD ON EXT.SPOUSE_DIST_ID =PSD.ID\n" +
            "                    LEFT JOIN LOC_VALUE PSC ON EXT.SPOUSE_DIV_ID =PSC.ID\n" +
            "                    WHERE E.ID =:hrEmpId ", nativeQuery = true)
    Optional<IHrEmpBasicInfo> getHrEmpBasicInfo(@Param("hrEmpId") Long hrEmpId);

    @Query(value = "select sum(LB.QTY) as total from LEAVE_OPENING_BALANCE LB \n" +
            "            JOIN LEAVE_TYPE LT ON LT.ID = LB.LEAVE_TYPE_ID\n" +
            "            where LB.HR_EMP_ID=? AND LT.CODE IN ('CL') \n" +
            "            and LB.ON_DATE between to_date((sysdate-(TO_CHAR(sysdate, 'DDD')-1))) and to_date(SYSDATE) \n" +
            "            group by LB.HR_EMP_ID ", nativeQuery = true)
    Optional<IHrEmpLeaveTotal> getLeaveTotal(@Param("hrEmpId") Long hrEmpId);

    @Procedure(procedureName = "SP_LEAVE_QUOTA_SUMMARY")
    void spCallProcedure(@Param("P_UNIT_ID") Long unitId, @Param("P_FROM_DATE") String fromDate, @Param("P_TO_DATE") String toDate);

    @Query(value = "select summ.L_DATE LDate, summ.RESTRICT, summ.RESERVED, summ.TOTAL, summ.USED, summ.UNIT_ID UnitId, summ.STATUS from TMP_LEAVE_QUOTA_SUMMARY summ", nativeQuery = true)
    List<ICalenderDto> spCall();

    @Modifying
    @javax.transaction.Transactional
    @Query(value = "TRUNCATE TABLE TMP_LEAVE_QUOTA_SUMMARY", nativeQuery = true)
    void deleteData();

    @Modifying
    @Transactional
    @Query(value = "update BaseLeaveApp ll set ll.approved.approvalStatus = :approvalStatus where ll.id in (:leaveIds) ")
    void updateLeaveApprovalStatusNoteSheet(@Param("leaveIds") List<Long> leaveIds, @Param("approvalStatus") ApprovalStatus approvalStatus);

    @Query(value = "select h.rankId from HrEmp h where h.id=:employeeCode")
    Long getEmpRank(@Param("employeeCode") Long employeeCode);

    @Query(value = "select max(da.ARRIVAL_MOMENT) from BASE_LEAVE_APP bla\n" +
            "join DEPARTURE_ARRIVAL da on bla.HR_EMP_ID=da.HR_EMP_ID\n" +
            "where da.HR_EMP_ID=:employeeCode", nativeQuery = true)
    String getLastReturnDateFromDepArr(@Param("employeeCode") Long employeeCode);

    @Query(value = "select max(cc.ARRIVAL_MOMENT) from BASE_LEAVE_APP bla\n" +
            "join CC cc on bla.HR_EMP_ID=cc.HR_EMP_ID\n" +
            "where cc.HR_EMP_ID=:employeeCode", nativeQuery = true)
    String getLastReturnDateFromCC(Long employeeCode);


    @Query(value = "select case\n" +
            "            when (select DESG_ID from Position where id = :id) is not null\n" +
            "                then (select d.NAME_NATIVE as desgOrRank\n" +
            "                      from Position pa\n" +
            "                               right join DESG D on pa.DESG_ID = D.ID\n" +
            "                      where pa.id = :id)\n" +
            "            else (select r.NAME_NATIVE as desgOrRank\n" +
            "                  from Position pa\n" +
            "                           right join RANK r on pa.RANK_ID = r.ID\n" +
            "                  where pa.id = :id) end\n" +
            " from dual", nativeQuery = true)
    String getEmpDesgOrRank(@Param("id") Long id);

    @Query(value = "(select un.UNIT_NAME_NATIVE||','||(select UNIT_NAME_NATIVE from unit\n" +
            "where id=(select DIS_UNIT_VALUE_ID from unit where id=un.ID))\n" +
            "from UNIT un where un.ID=:id)", nativeQuery = true)
    String getEmpSignatoryUnit(@Param("id") Long id);

    @Query(value = "select (select UNIT_NAME_NATIVE from unit where id=(select DIS_UNIT_VALUE_ID\n" +
            "from unit where id=U.ID))\n" +
            "from UNIT U where U.ID=:id", nativeQuery = true)
    String getToPositionUnitShort(@Param("id") Long id);

    @Query(value = "select distinct oo from BaseLeaveApp oo " +
            "left join BaseLeaveClPlApp cl on oo.id = cl.id  join Cc c on c.mstId=oo.id and concat(c.docType,'')='LEAVE_APP' " +
            "where 1=1 and oo.approved.approvalStatus = 'OFFICE_ORDERED' " +
            "and concat(oo.leaveStatus, '') in ('START', 'ON_LEAVE') " +
            "and concat(oo.dispatchStatus,'') = 'APPROVED' " +
            "and(:leaveType is null or oo.leaveType.id = :leaveType) " +
            "and  oo.hrEmpId in (:hrEmpIdList) " +
            " and (:unitId is null or oo.position.unit.id = :unitId) " +
            " and  coalesce(cl.plFromDate,oo.fromDate) between TO_DATE(:fromDate, 'YYYY-MM-DD' ) and TO_DATE(:toDate, 'YYYY-MM-DD')   " +
            " order by oo.approved.approvedDate desc ")
    List<BaseLeaveApp> findAllOfficeOrderedList(
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            @Param("leaveType") Long leaveType,
            @Param("unitId") Long unitId,
            @Param("hrEmpIdList") List<Long> hrEmpIdList
    );

    @Query(value = "select p.RPT_DISPLAY_NAME\n" +
            "from POSITION p where p.ID=:id", nativeQuery = true)
    String getReportPosition(@Param("id") Long id);

//    @Query(value = "select TOTAL_CL_DAYS\n" +
//            "from LEAVE_CLPL_APP where ID=:id", nativeQuery = true)
//    String findClPlData(@Param("id") Long id);

    @Query(value = "select TOTAL_PL_DAYS as totalDays, TO_CHAR(FROM_DATE_PL, 'YYYY-MM-DD') as fromDate," +
            " TO_CHAR(TO_DATE_PL, 'YYYY-MM-DD') as toDate\n" +
            "from LEAVE_CLPL_APP where ID=:id", nativeQuery = true)
    clPlInterfaceDto findPlData(@Param("id") Long id);

    @Query(value = "select TOTAL_CL_DAYS\n" +
            "from LEAVE_CLPL_APP where ID=:id", nativeQuery = true)
    String findClTotalDays(@Param("id") Long id);


    interface clPlInterfaceDto {
        public String getTotalDays();

        public String getFromDate();

        public String getToDate();
    }
}