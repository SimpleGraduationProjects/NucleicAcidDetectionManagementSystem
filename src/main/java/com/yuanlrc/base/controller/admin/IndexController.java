package com.yuanlrc.base.controller.admin;

import com.yuanlrc.base.annotion.ValidateEntity;
import com.yuanlrc.base.bean.*;
import com.yuanlrc.base.constant.SessionConstant;
import com.yuanlrc.base.entity.admin.Community;
import com.yuanlrc.base.entity.admin.Migrant;
import com.yuanlrc.base.entity.admin.Resident;
import com.yuanlrc.base.entity.admin.TestRecord;
import com.yuanlrc.base.service.admin.CommunityService;
import com.yuanlrc.base.service.admin.MigrantService;
import com.yuanlrc.base.service.admin.ResidentService;
import com.yuanlrc.base.service.admin.TestRecordService;
import com.yuanlrc.base.util.SessionUtil;
import com.yuanlrc.base.util.StringUtil;
import com.yuanlrc.base.util.ValidateEntityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.management.relation.RelationService;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/index")
public class IndexController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private ResidentService residentService;

    @Autowired
    private MigrantService migrantService;

    @Autowired
    private TestRecordService testRecordService;


    @GetMapping("/index")
    public String index(Model model)
    {

        return "admin/index/index";
    }

    @GetMapping("/add_migrant")
    public String addMigrant(Model model)
    {
        model.addAttribute("communtiys", communityService.findAll());
        return  "admin/index/add_migrant";
    }

    @PostMapping("/add_migrant")
    @ResponseBody
    public Result<Boolean> addMigrant(Migrant migrant, Long communtiyId)
    {
        Community find = communityService.find(communtiyId);
        if(find == null)
            return Result.error(CodeMsg.ADMIN_COMMUNITY_NOT_FOUND_ERROR);

        migrant.setCommunity(find);
        migrant.setStatus(UserStatus.AUDIT);

        //?????????????????????
        if(!StringUtil.isCard(migrant.getCardNumber()))
            return Result.error(CodeMsg.COMMON_IDCARD_FORMAET_ERROR);


        CodeMsg codeMsg = ValidateEntityUtil.validate(migrant);
        if(codeMsg.getCode() != CodeMsg.SUCCESS.getCode())
            return Result.error(codeMsg);


        //?????????????????????????????????????????????
        Resident resident = residentService.findByCardNumberAndCommunityCityAndIsDel
                (migrant.getCardNumber(), migrant.getCommunity().getCity(), IsDel.NOT_DEL);
        if(resident != null)
            return Result.error(CodeMsg.ADMIN_HAS_RESIDENT_ERROR);

        //??????????????????????????????????????????????????????
        Migrant findMigrant = migrantService.findByCardNumberAndCommunityIdAndIsDelAndStatus(migrant.getCardNumber(), find.getId());
        if(findMigrant != null)
            return Result.error(CodeMsg.ADMIN_ADD_MIGRANT_ERROR);

        if(migrantService.save(migrant) == null)
            return Result.error(CodeMsg.ADMIN_MIGRANT_ADD_ERROR);


        return Result.success(true);
    }

    /**
     * ?????????????????????
     * @return
     */
    @PostMapping("/search")
    @ResponseBody
    public Result<Boolean> search(String cardNumber)
    {
        //?????????????????????
        if(!StringUtil.isCard(cardNumber))
            return Result.error(CodeMsg.COMMON_IDCARD_FORMAET_ERROR);

        //?????????????????????
        Resident resident = residentService.findByCardNumberAndIsDel(cardNumber);

        //?????????????????????
        Migrant migrant = migrantService.findByCardNumberAndIsDel(cardNumber);

        //???????????????????????????
        List<TestRecord> testRecords = testRecordService.findByCardNumber(cardNumber);


        if(resident == null && migrant == null && testRecords.size() == 0)
            return Result.error(CodeMsg.ADMIN_NOT_FOUND_INFO);

        //?????????????????????
        SessionUtil.set(SessionConstant.SESSION_SEARCH_IDCARD, cardNumber);

        return Result.success(true);
    }


    /**
     * ???????????????????????????
     * @param model
     * @return
     */
    @GetMapping("/info")
    public String info(Model model)
    {
        String cardNumber = SessionUtil.getCardNumber();
        if(cardNumber == null)
            return "redirect:index";

        //?????????????????????
        Resident resident = residentService.findByCardNumberAndIsDel(cardNumber);

        //?????????????????????
        Migrant migrant = migrantService.findByCardNumberAndIsDel(cardNumber);

        //???????????????????????????
        List<TestRecord> testRecords = testRecordService.findByCardNumber(cardNumber);

        model.addAttribute("resident", resident);
        model.addAttribute("migrant", migrant);
        model.addAttribute("testRecords", testRecords);
        model.addAttribute("male", Sex.MALE);
        model.addAttribute("userStatus", UserStatus.values());
        model.addAttribute("notPass", UserStatus.NOT_PASS);

        return "admin/index/info";
    }

    @GetMapping("/detail")
    public String detail(Long id, Model model)
    {
        String cardNumber = SessionUtil.getCardNumber();
        if(cardNumber == null)
            return "redirect:index";

        TestRecord testRecord = testRecordService.find(id); //????????????

        model.addAttribute("testRecord", testRecord);
        model.addAttribute("male", Sex.MALE);
        model.addAttribute("notPass", UserStatus.NOT_PASS);

        return "admin/index/detail";
    }

    /**
     * ??????????????????
     * @param model
     * @param id
     * @return
     */
    @GetMapping("/edit_migrant")
    public String edit(Model model, Long id)
    {
        String cardNumber = SessionUtil.getCardNumber();

        Migrant migrant = migrantService.find(id);

        //????????????????????????????????????
        if(cardNumber == null || migrant == null || (!cardNumber.equals(migrant.getCardNumber())))
            return "redirect:info;";

        //?????????????????????????????????
        if(migrant.getStatus().getCode() != UserStatus.NOT_PASS.getCode())
            return "redirect:info;";

        migrant.headPicToImages();

        model.addAttribute("migrant", migrant);
        model.addAttribute("communtiys", communityService.findAll());
        model.addAttribute("sexList", Sex.values());

        return "admin/index/edit_migrant";
    }

    /**
     * ??????????????????
     * @param migrant
     * @param communtiyId
     * @return
     */
    @PostMapping("/edit_migrant")
    @ResponseBody
    public Result<Boolean> editMigrant(Migrant migrant, Long communtiyId)
    {
        String cardNumber = SessionUtil.getCardNumber();

        Migrant findById = migrantService.find(migrant.getId());

        //????????????????????????????????????
        if(cardNumber == null || findById == null || (!cardNumber.equals(findById.getCardNumber())))
            return Result.error(CodeMsg.ADMIN_EDIT_MIGRANT_ERROR);

        //?????????????????????????????????
        if(findById.getStatus().getCode() != UserStatus.NOT_PASS.getCode())
            return Result.error(CodeMsg.ADMIN_EDIT_MIGRANT_ERROR);


        findById.setStatus(UserStatus.AUDIT);
        findById.setName(migrant.getName());
        findById.setSex(migrant.getSex());
        findById.setAddress(migrant.getAddress());
        findById.setCensusAddress(migrant.getCensusAddress());

        CodeMsg codeMsg = ValidateEntityUtil.validate(findById);
        if(codeMsg.getCode() != CodeMsg.SUCCESS.getCode())
            return Result.error(codeMsg);

        if(migrantService.save2(findById) == null)
            return Result.error(CodeMsg.ADMIN_EDIT_MIGRANT_ERROR);


        return Result.success(true);
    }

    /**
     * ??????????????????
     * @param model
     * @return
     */
    @GetMapping("/add_test_record")
    public String addTestRecord(Model model)
    {
        String cardNumber = SessionUtil.getCardNumber();

        //?????????????????????
        Resident resident = residentService.findByCardNumberAndIsDel(cardNumber);

        //?????????????????????
        Migrant migrant = migrantService.findByCardNumberAndIsDelAndStatus(cardNumber);

        if(cardNumber == null)
            return "redirect:info;";

        List<Community> communities = new ArrayList<>();
        if(resident != null)
            communities.add(resident.getCommunity());

        if(migrant != null)
            communities.add(migrant.getCommunity());

        model.addAttribute("communities", communities);

        return "admin/index/add_test_record";
    }

    @PostMapping("/add_test_record")
    @ResponseBody
    public Result<Boolean> addTestRecord(TestRecord testRecord, Long communtiyId)
    {
        String cardNumber = SessionUtil.getCardNumber();
        if(cardNumber == null)
            return Result.error(CodeMsg.ADMIN_CARDNUMBER_SESSION_ERROR);

        Community community = communityService.find(communtiyId);
        if(community == null)
            return Result.error(CodeMsg.ADMIN_COMMUNITY_NOT_FOUND_ERROR);

        //?????????????????????
        Resident resident = residentService.findByCardNumberAndCommunityIdAndIsDel(cardNumber, communtiyId, IsDel.NOT_DEL);

        //?????????????????????
        Migrant migrant = migrantService.findByCardNumberAndCommunityIdAndIsDelAndStatus(cardNumber, communtiyId);

        if(resident == null && migrant == null)
            return Result.error(CodeMsg.ADMIN_PLEASE_ADD_INFO_ERROR);

        testRecord.setCardNumber(cardNumber);
        testRecord.setCommunity(community);
        testRecord.setStatus(UserStatus.AUDIT);
        if(resident == null)
        {
            //????????????
            testRecord.setAddress(migrant.getAddress());
            testRecord.setName(migrant.getName());
            testRecord.setSex(migrant.getSex());
        }
        else
        {
            //????????????
            testRecord.setAddress(resident.getAddress());
            testRecord.setName(resident.getName());
            testRecord.setSex(resident.getSex());
        }

        CodeMsg codeMsg = ValidateEntityUtil.validate(testRecord);
        if(codeMsg.getCode() != CodeMsg.SUCCESS.getCode())
            return Result.error(codeMsg);

        //????????????????????????
        if(!StringUtil.isMobile(testRecord.getGatherMobile()))
            return Result.error(CodeMsg.ADMIN_PHONE_FORMAET_ERROR);

        if(testRecordService.save(testRecord) == null)
            return Result.error(CodeMsg.ADMIN_TESTRECORD_SAVE_ERROR);

        return Result.success(true);
    }
}
