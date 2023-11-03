package com.divatt.designer.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.divatt.designer.config.JWTConfig;
import com.divatt.designer.constant.MessageConstant;
import com.divatt.designer.constant.RestTemplateConstants;
import com.divatt.designer.dto.DesignerContactDto;
import com.divatt.designer.entity.DesignerContactEntity;
import com.divatt.designer.entity.LoginEntity;
import com.divatt.designer.entity.Measurement;
import com.divatt.designer.entity.UserDesignerEntity;
import com.divatt.designer.entity.product.ProductMasterEntity2;
import com.divatt.designer.entity.profile.BankDetails;
import com.divatt.designer.entity.profile.DesignerFundsAccount;
import com.divatt.designer.entity.profile.DesignerLoginEntity;
import com.divatt.designer.entity.profile.DesignerPersonalInfoEntity;
import com.divatt.designer.entity.profile.DesignerProfile;
import com.divatt.designer.entity.profile.DesignerProfileEntity;
import com.divatt.designer.entity.profile.Geometry;
import com.divatt.designer.entity.profile.PayOutDTO;
import com.divatt.designer.entity.profile.ProfileImage;
import com.divatt.designer.entity.profile.RazorpayX;
import com.divatt.designer.entity.profile.SocialProfile;
import com.divatt.designer.exception.CustomException;
import com.divatt.designer.helper.CustomFunction;
import com.divatt.designer.helper.EmailSenderThread;
import com.divatt.designer.repo.DatabaseSeqRepo;
import com.divatt.designer.repo.DesignerLoginRepo;
import com.divatt.designer.repo.DesignerPersonalInfoRepo;
import com.divatt.designer.repo.DesignerProfileRepo;
import com.divatt.designer.repo.MeasurementRepo;
import com.divatt.designer.repo.ProductRepo2;
import com.divatt.designer.repo.ProductRepository;
import com.divatt.designer.response.GlobalResponce;
import com.divatt.designer.services.SequenceGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.JsonNode;

@RestController
@RequestMapping("/designer")
public class ProfileContoller {

	@Autowired
	private SequenceGenerator sequenceGenarator;

	@Autowired
	private DesignerProfileRepo designerProfileRepo;

	@Autowired
	private DesignerLoginRepo designerLoginRepo;

	@Autowired
	private SequenceGenerator sequenceGenerator;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	ProductController2 productController2;

	@Autowired
	private DesignerPersonalInfoRepo designerPersonalInfoRepo;

	@Autowired
	private ProductRepository productRepo;

	@Autowired
	private DatabaseSeqRepo databaseSeqRepo;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ProductRepo2 productRepo2;

	@Autowired
	private MeasurementRepo measurementRepo;

	@Autowired
	private MongoOperations mongoOperations;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileContoller.class);

	@Autowired
	private JWTConfig jwtConfig;

	@Autowired
	private CustomFunction customFunction;

	@Autowired
	private TemplateEngine templateEngine;

	@Value("${DESIGNER}")
	private String DESIGNER_SERVICE;

	@Value("${AUTH}")
	private String AUTH_SERVICE;

	@Value("${ADMIN}")
	private String ADMIN_SERVICE;

	@Value("${USERS}")
	private String USER_SERVICES;

	@Value("${redirectURL}")
	private String redirectURL;

	@Value("${GOOGLE_MAP_APIKEY}")
	private String GOOGLE_MAP_APIKEY;

	@Value("${DISTANCE}")
	private String Max_Map_Distance;

	protected String getRandomString() {
		String SALTCHARS = "1234567890";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < 4) {
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;
	}

	@GetMapping("/{id}")

	public ResponseEntity<?> getDesigner(@PathVariable Long id) {
		try {
			Optional<DesignerProfileEntity> findById = designerProfileRepo.findBydesignerId(id);
			if (findById.isPresent()) {
				DesignerProfileEntity designerProfileEntity = findById.get();
				try {
					if (designerProfileEntity.getSocialProfile() == null)
						designerProfileEntity.setSocialProfile(new SocialProfile());
				} catch (Exception e) {
					designerProfileEntity.setSocialProfile(new SocialProfile());
				}
				try {
					// DesignerPersonalInfoEntity designerPersonalInfoEntity =
					// designerPersonalInfoRepo.findByDesignerId(id).get();
					DesignerLoginEntity designerLoginEntity = designerLoginRepo.findById(id).get();
					designerProfileEntity.setAccountStatus(designerLoginEntity.getAccountStatus());
					designerProfileEntity.setProfileStatus(designerLoginEntity.getProfileStatus());
					designerProfileEntity.setIsDeleted(designerLoginEntity.getIsDeleted());
					designerProfileEntity.setIsProfileCompleted(designerLoginEntity.getIsProfileCompleted());
					designerLoginEntity.setDesignerCurrentStatus(designerLoginEntity.getDesignerCurrentStatus());
					if (designerPersonalInfoRepo.findByDesignerId(id).isPresent()) {
						designerProfileEntity
								.setDesignerPersonalInfoEntity(designerPersonalInfoRepo.findByDesignerId(id).get());
					} else {
						designerProfileEntity
								.setDesignerPersonalInfoEntity(findById.get().getDesignerPersonalInfoEntity());
					}
					designerProfileEntity.setProductCount(
							productRepo2.countByIsDeletedAndAdminStatusAndDesignerIdAndIsActiveAndSohNot(false,
									"Approved", id.intValue(), true, 0));
					designerProfileEntity.setDesignerCurrentStatus(designerLoginEntity.getDesignerCurrentStatus());
					org.json.simple.JSONObject countData = countData(id);
					String followerCount = countData.get("FollowersData").toString();
					designerProfileEntity.setFollowerCount(Integer.parseInt(followerCount));
//				ResponseEntity<DesignerProfileEntity> postForEntity = restTemplate .postForEntity
//						                                             (ADMIN_SERVICE+RestTemplateConstants.ADMIN_ADD_CONTACTS,null,DesignerProfileEntity.class );
//				DesignerProfileEntity body = postForEntity.getBody();
//				designerProfileRepo.save(body);
				} catch (Exception e1) {
					throw new CustomException(e1.getMessage());
				}
				List<Measurement> findByDesignerId = measurementRepo.findByDesignerId(id.intValue());
				if (findByDesignerId.size() > 0) {
					findByDesignerId.stream().forEach(measurement -> {
						if (measurement.getMeasurementsMen() != null) {
							designerProfileEntity.setMenChartData(measurement);
						} else if (measurement.getMeasurementsWomen() != null) {
							designerProfileEntity.setWomenChartData(measurement);
						}
					});
				} else {
					designerProfileEntity.setWomenChartData(null);
					designerProfileEntity.setMenChartData(null);
				}
				return ResponseEntity.ok(designerProfileEntity);

			} else {
				throw new CustomException(MessageConstant.DESIGNER_NOT_FOUND.getMessage());
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/user/{id}")
	public ResponseEntity<?> getUserDesigner(@PathVariable Long id) {
		try {
			DesignerLoginEntity designerLoginEntity = new DesignerLoginEntity();
			Optional<DesignerLoginEntity> findById = designerLoginRepo.findById(id);
			if (findById.get().getIsProfileCompleted() == null) {
				findById.get().setIsProfileCompleted(true);
			}
			if (!findById.isPresent())
				throw new CustomException(MessageConstant.PROFILE_NOT_COMPLETED.getMessage());
			if (findById.get().getIsProfileCompleted()) {
				designerLoginEntity = findById.get();
				designerLoginEntity.setDesignerProfileEntity(designerProfileRepo
						.findBydesignerId(Long.parseLong(designerLoginEntity.getdId().toString())).get());
				designerLoginEntity.setProductCount(
						this.productRepo2.countByIsDeletedAndAdminStatusAndDesignerIdAndIsActiveAndSohNot(false,
								"Approved", findById.get().getdId().intValue(), true, 0));
			}
			return ResponseEntity.ok(designerLoginEntity);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@PostMapping("/add")
	public ResponseEntity<?> addDesigner(@Valid @RequestBody DesignerProfileEntity designerProfileEntity) {
		try {

			Optional<DesignerProfileEntity> findByBoutiqueName = designerProfileRepo
					.findByBoutiqueName(designerProfileEntity.getBoutiqueProfile().getBoutiqueName());

			if (findByBoutiqueName.isPresent()) {

				throw new CustomException(MessageConstant.BOUTIQUE_NAME.getMessage());
			}
			if (findByBoutiqueName.isPresent()) {
				throw new CustomException(MessageConstant.BOUTIQUE_NAME.getMessage());
			}
			Optional<DesignerLoginEntity> findByDesignerProfileEmail = designerLoginRepo
					.findByEmailAndAccountStatusNot(designerProfileEntity.getDesignerProfile().getEmail(), "INACTIVE");

			if (!findByDesignerProfileEmail.isPresent()
					|| findByDesignerProfileEmail.get().getAccountStatus().equals("INACTIVE")) {

				Query query = new Query();
				query.limit(1);
				query.with(Sort.by(Sort.DEFAULT_DIRECTION.DESC, "dId"));

				List<DesignerLoginEntity> designerLoginData = mongoOperations.find(query, DesignerLoginEntity.class);

				String randomId = this.getRandomString();
				Long dUid = 10L;
				if (designerLoginData.size() > 0) {
					dUid = (designerLoginData.get(0).getdId()) + 1;
				}
				String uid = randomId + dUid;
				ResponseEntity<String> forEntity = restTemplate
						.getForEntity(AUTH_SERVICE + RestTemplateConstants.PRESENT_DESIGNER
								+ designerProfileEntity.getDesignerProfile().getEmail(), String.class);
				DesignerLoginEntity designerLoginEntity = new DesignerLoginEntity();
				JSONObject jsObj = new JSONObject(forEntity.getBody());
				if ((boolean) jsObj.get("isPresent") && jsObj.get("role").equals("DESIGNER"))
					throw new CustomException("Email already present");
				if ((boolean) jsObj.get("isPresent") && jsObj.get("role").equals("USER")) {
					ResponseEntity<String> forEntity2 = restTemplate.getForEntity(AUTH_SERVICE
							+ RestTemplateConstants.INFO_USER + designerProfileEntity.getDesignerProfile().getEmail(),
							String.class);
					designerLoginEntity.setUserExist(forEntity2.getBody());
				}

				designerLoginEntity.setdId((long) sequenceGenerator.getNextSequence(DesignerLoginEntity.SEQUENCE_NAME));
				designerLoginEntity.setEmail(designerProfileEntity.getDesignerProfile().getEmail());
				designerLoginEntity.setPassword(
						bCryptPasswordEncoder.encode(designerProfileEntity.getDesignerProfile().getPassword()));
				designerLoginEntity.setIsDeleted(false);
				designerLoginEntity.setAccountStatus("INACTIVE");
				designerLoginEntity.setProfileStatus("new");
				designerLoginEntity.setIsProfileCompleted(false);
				designerLoginEntity.setDesignerCurrentStatus("Online");
				designerLoginEntity.setProductCount(0);
				designerLoginEntity.setFollwerCount(0);
				designerLoginEntity.setUid(uid);
				if (findByDesignerProfileEmail.isPresent()) {
					designerLoginEntity.setdId(findByDesignerProfileEmail.get().getdId());
				}

				if (designerLoginRepo.save(designerLoginEntity) != null) {
					designerProfileEntity.setDesignerId(Long.parseLong(designerLoginEntity.getdId().toString()));
					designerProfileEntity
							.setId((long) sequenceGenerator.getNextSequence(DesignerProfileEntity.SEQUENCE_NAME));
					designerProfileEntity.setIsProfileCompleted(false);
					DesignerProfile designerProfile = designerProfileEntity.getDesignerProfile();
					designerProfile.setPassword(
							bCryptPasswordEncoder.encode(designerProfileEntity.getDesignerProfile().getPassword()));
					designerProfileEntity.setDesignerProfile(designerProfile);
					designerProfileEntity.setDesignerCurrentStatus("Online");
					designerProfileEntity.setUid(uid);
					if (findByBoutiqueName.isPresent()) {
						designerProfileEntity.setId(findByBoutiqueName.get().getId());
					}
					designerProfileRepo.save(designerProfileEntity);
				}
				String designerName = designerProfileEntity.getDesignerName();
				String designerEmail = designerProfileEntity.getDesignerProfile().getEmail();

				URI uri = URI.create(redirectURL
						+ Base64.getEncoder().encodeToString(designerLoginEntity.getEmail().toString().getBytes()));
				Context context = new Context();
				context.setVariable("designerName", designerName);
				context.setVariable("uri", uri);
				String htmlContent = templateEngine.process("designerRegistration.html", context);
				EmailSenderThread emailSenderThread = new EmailSenderThread(designerEmail, "Successfully Registration",
						htmlContent, true, null, restTemplate, AUTH_SERVICE);
				emailSenderThread.start();

				return ResponseEntity.ok(new GlobalResponce(MessageConstant.SUCCESS.getMessage(),
						MessageConstant.REGISTERED.getMessage(), 200));

			}

			else {
				throw new CustomException(MessageConstant.EMAIL_EXIST.getMessage());

//			} else {
//				throw new CustomException(MessageConstant.EMAIL_ALREADY_EXIST.getMessage());
//			
//			

			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@PutMapping("/update")
	public ResponseEntity<?> updateDesigner(@RequestBody DesignerLoginEntity designerLoginEntity) {

		Optional<DesignerLoginEntity> findById = designerLoginRepo.findById(designerLoginEntity.getdId());
		if (!findById.isPresent())
			throw new CustomException(MessageConstant.DETAILS_NOT_FOUND.getMessage());
		else {
			DesignerLoginEntity designerLoginEntityDB = findById.get();

			designerProfileRepo.save(customFunction.designerProfileEntity(designerLoginEntity));

			if (designerLoginEntity.getProfileStatus().equals("SUBMITTED")
					|| designerLoginEntity.getProfileStatus().equals("COMPLETED")
					|| designerLoginEntity.getProfileStatus().equals("SAVED")) {

				Optional<DesignerPersonalInfoEntity> profileInfo = designerPersonalInfoRepo
						.findByDesignerId(designerLoginEntity.getdId());
				if (profileInfo.orElse(null) != null) {
					DesignerPersonalInfoEntity infoEntity = designerPersonalInfoRepo
							.findByDesignerId(designerLoginEntity.getdId()).get();

					DesignerPersonalInfoEntity designerPersonalInfoEntity = new DesignerPersonalInfoEntity();
					designerPersonalInfoEntity.setId(infoEntity.getId());
					designerPersonalInfoEntity.setDesignerId(designerLoginEntity.getdId());
					designerPersonalInfoEntity.setBankDetails(designerLoginEntity.getDesignerProfileEntity()
							.getDesignerPersonalInfoEntity().getBankDetails());
					designerPersonalInfoEntity.setDesignerDocuments(designerLoginEntity.getDesignerProfileEntity()
							.getDesignerPersonalInfoEntity().getDesignerDocuments());
					designerPersonalInfoRepo.save(designerPersonalInfoEntity);
				}
			}
			// Old
			designerLoginEntityDB.setProfileStatus(designerLoginEntity.getProfileStatus());
			designerLoginEntityDB.setCategories(designerLoginEntity.getCategories());
			designerLoginEntityDB.setAccountStatus("ACTIVE");
			designerLoginEntityDB.setIsDeleted(designerLoginEntity.getIsDeleted());
			designerLoginEntityDB.setIsProfileCompleted(designerLoginEntity.getIsProfileCompleted());
			designerLoginEntityDB.setUid(designerLoginEntity.getUid());
			LOGGER.info("string" + this.getDesigner(designerLoginEntityDB.getdId()).getBody());
			Object string = this.getDesigner(designerLoginEntityDB.getdId()).getBody();
			LOGGER.info("string" + string);
			String designerId = null;
			ObjectMapper mapper = new ObjectMapper();
			try {
				designerId = mapper.writeValueAsString(string);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}

			JsonNode jsonNode = new JsonNode(designerId);
			String string2 = jsonNode.getObject().get("designerName").toString();
			String email = designerLoginEntityDB.getEmail();
			designerLoginRepo.save(designerLoginEntityDB);
			try {
//				LoginEntity forEntity = restTemplate.getForEntity(ADMIN_SERVICE + RestTemplateConstants.ADMIN_ROLE_NAME
//						+ MessageConstant.ADMIN_ROLES.getMessage(), LoginEntity.class).getBody();

				ResponseEntity<List<LoginEntity>> responseData = restTemplate.exchange(
						ADMIN_SERVICE + RestTemplateConstants.ADMIN_ROLE_NAME
								+ MessageConstant.ADMIN_ROLES.getMessage(),
						HttpMethod.GET, null, new ParameterizedTypeReference<List<LoginEntity>>() {
						});
				List<LoginEntity> loginData = responseData.getBody();
				String email2 = loginData.get(0).getEmail();
				LOGGER.info(email2);
				if (designerLoginEntity.getProfileStatus().equals("REJECTED")) {
					designerLoginEntityDB.setAdminComment(designerLoginEntity.getAdminComment());
					Context context = new Context();
					context.setVariable("designerName", string2);
					context.setVariable("adminComment", designerLoginEntity.getAdminComment());
					String htmlContent = templateEngine.process("designerRejected.html", context);
					EmailSenderThread emailSenderThread = new EmailSenderThread(email, "Designer rejected", htmlContent,
							true, null, restTemplate, AUTH_SERVICE);
					emailSenderThread.start();
					String htmlContent1 = templateEngine.process("designerRejectedAdmin.html", context);
					EmailSenderThread emailSenderThread1 = new EmailSenderThread(email2, "Designer rejected",
							htmlContent1, true, null, restTemplate, AUTH_SERVICE);
					emailSenderThread1.start();
				} else {
					Context context = new Context();
					context.setVariable("designerName", string2);
					String htmlContent = templateEngine.process("designerUpdate.html", context);
					EmailSenderThread emailSenderThread = new EmailSenderThread(email, "Designer updated", htmlContent,
							true, null, restTemplate, AUTH_SERVICE);
					emailSenderThread.start();
					String htmlContent1 = templateEngine.process("designerUpdateAdmin.html", context);
					EmailSenderThread emailSenderThread1 = new EmailSenderThread(email2, "Designer updated",
							htmlContent1, true, null, restTemplate, AUTH_SERVICE);
					emailSenderThread1.start();
				}
			} catch (Exception e) {
				throw new CustomException(e.getMessage());
			}
			return ResponseEntity.ok(new GlobalResponce(MessageConstant.SUCCESS.getMessage(),
					MessageConstant.UPDATED.getMessage(), 200));
		}
	}

	@PutMapping("/profile/update")
	public ResponseEntity<?> updateDesignerProfile(@Valid @RequestBody DesignerProfileEntity designerProfileEntity) {
		try {
			@Valid
			DesignerPersonalInfoEntity designerPersonalInfoEntity = designerProfileEntity
					.getDesignerPersonalInfoEntity();
			Optional<DesignerPersonalInfoEntity> findByDesignerId = designerPersonalInfoRepo
					.findByDesignerId(designerProfileEntity.getDesignerId());
			if (findByDesignerId.isPresent()) {
				designerPersonalInfoEntity.setId(findByDesignerId.get().getId());
			} else {
				designerPersonalInfoEntity
						.setId((long) sequenceGenerator.getNextSequence(DesignerPersonalInfoEntity.SEQUENCE_NAME));
				designerPersonalInfoEntity.setDesignerId(designerProfileEntity.getDesignerId());

			}

			designerPersonalInfoRepo.save(designerPersonalInfoEntity);

			Measurement menChartData = designerProfileEntity.getMenChartData();
			menChartData.set_id(sequenceGenarator.getNextSequence(Measurement.SEQUENCE_NAME));
			menChartData.setCreatedOn(new Date());
			Measurement womenChartData = designerProfileEntity.getWomenChartData();
			womenChartData.set_id(sequenceGenarator.getNextSequence(Measurement.SEQUENCE_NAME));
			womenChartData.setCreatedOn(new Date());
			measurementRepo.save(menChartData);
			measurementRepo.save(womenChartData);
		} catch (Exception e) {
			throw new CustomException(MessageConstant.CHECK_FIELDS.getMessage());
		}

		Optional<DesignerLoginEntity> findById = designerLoginRepo.findById(designerProfileEntity.getDesignerId());
		if (!findById.isPresent())
			throw new CustomException(MessageConstant.DETAILS_NOT_FOUND.getMessage());
		else {

			Optional<DesignerProfileEntity> findBydesignerId = designerProfileRepo
					.findBydesignerId(findById.get().getdId());
			if (!findBydesignerId.isPresent())
				throw new CustomException(MessageConstant.DETAILS_NOT_FOUND.getMessage());

			DesignerProfile designerProfile = designerProfileEntity.getDesignerProfile();
			designerProfile.setEmail(findById.get().getEmail());
			designerProfile.setPassword(findById.get().getPassword());
			designerProfile.setUid(findById.get().getUid());
			designerProfile.setProfilePic(designerProfileEntity.getDesignerProfile().getProfilePic());

			DesignerProfileEntity designerProfileEntityDB = findBydesignerId.get();

			designerProfileEntityDB.setBoutiqueProfile(designerProfileEntity.getBoutiqueProfile());
			designerProfileEntityDB.setDesignerProfile(designerProfile);
			designerProfileEntityDB.setSocialProfile(designerProfileEntity.getSocialProfile());
			designerProfileEntityDB.setUid(designerProfileEntity.getUid());
			Geometry geometry = designerProfileEntity.getGeometry();
			geometry.setType("Point");
			designerProfileEntityDB.setGeometry(geometry);

			designerProfileRepo.save(designerProfileEntityDB);
			DesignerLoginEntity designerLoginEntityDB = findById.get();
			designerLoginEntityDB.setProfileStatus(designerProfileEntity.getProfileStatus());
			designerLoginEntityDB.setIsProfileCompleted(designerProfileEntity.getIsProfileCompleted());
			designerLoginEntityDB.setUid(designerProfileEntity.getUid());
			designerLoginRepo.save(designerLoginEntityDB);
			try {

				ResponseEntity<List<org.json.simple.JSONObject>> forEntity = restTemplate.exchange(
						ADMIN_SERVICE + RestTemplateConstants.ADMIN_ROLE_NAME
								+ MessageConstant.ADMIN_ROLES.getMessage(),
						HttpMethod.GET, null, new ParameterizedTypeReference<List<org.json.simple.JSONObject>>() {
						});
				List<org.json.simple.JSONObject> loginData = forEntity.getBody();

				String email2 = loginData.get(0).get("email").toString();

				String email = designerProfileEntity.getDesignerProfile().getEmail();
				Context context = new Context();
				context.setVariable("designerName", designerProfileEntity.getDesignerName());
				String htmlContent = templateEngine.process("designerUpdate.html", context);
				EmailSenderThread emailSenderThread = new EmailSenderThread(email, "Designer updated", htmlContent,
						true, null, restTemplate, AUTH_SERVICE);
				emailSenderThread.start();
				String htmlContent1 = templateEngine.process("designerUpdateAdmin.html", context);
				EmailSenderThread emailSenderThread1 = new EmailSenderThread(email2, "Designer updated", htmlContent1,
						true, null, restTemplate, AUTH_SERVICE);
				emailSenderThread1.start();
			} catch (Exception e) {
				throw new CustomException(e.getMessage());
			}
			return ResponseEntity.ok(new GlobalResponce(MessageConstant.SUCCESS.getMessage(),
					MessageConstant.UPDATED.getMessage(), 200));
		}
	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public Map<String, Object> getAll(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "DESC") String sort,
			@RequestParam(defaultValue = "createdOn") String sortName,
			@RequestParam(defaultValue = "false") Boolean isDeleted,
			@RequestParam(defaultValue = "") String profileStatus, @RequestParam(defaultValue = "") String keyword,
			@RequestParam Optional<String> sortBy) {

		try {
			return this.getDesignerProfDetails(page, limit, sort, sortName, isDeleted, keyword, sortBy, profileStatus);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@RequestMapping(value = "/redirect/{email}", method = RequestMethod.GET)
	public void method(HttpServletResponse httpServletResponse, @PathVariable("email") String email) {
		Optional<DesignerLoginEntity> findByEmail = designerLoginRepo
				.findByEmail(new String(Base64.getDecoder().decode(email)));

		if (findByEmail.isPresent()) {
			DesignerLoginEntity designerLoginEntity = findByEmail.get();
			if (designerLoginEntity.getAccountStatus().equals("INACTIVE"))
				designerLoginEntity.setAccountStatus("ACTIVE");
			designerLoginEntity.setProfileStatus("waitForApprove");
			designerLoginRepo.save(designerLoginEntity);
		}

		httpServletResponse.setHeader("Location", RestTemplateConstants.DESIGNER);
		httpServletResponse.setStatus(302);
	}

	@GetMapping("/userDesignerList")
	public ResponseEntity<?> userDesignertList() {
		try {
			long count = databaseSeqRepo.findById(DesignerLoginEntity.SEQUENCE_NAME).get().getSeq();
			Random rd = new Random();
			List<DesignerLoginEntity> designerLoginEntity = new ArrayList<>();
			List<DesignerLoginEntity> findAll = designerLoginRepo
					.findByIsDeletedAndAndIsProfileCompletedAndAccountStatusAndDesignerCurrentStatus(false, true,
							"ACTIVE", "Online");
			List<Integer> lst = new ArrayList<>();
			if (findAll.size() <= 15) {
				designerLoginEntity = findAll;

			} else {
				Boolean flag = true;

				while (flag) {
					int nextInt = rd.nextInt((int) count);
					for (DesignerLoginEntity obj : findAll) {

						if (obj.getdId() == nextInt && !lst.contains(nextInt)) {
							lst.add(nextInt);
							designerLoginEntity.add(obj);
						}
						if (designerLoginEntity.size() > 14)
							flag = false;
					}
				}
			}

			Stream<DesignerLoginEntity> map = designerLoginEntity.stream().map(e -> {
				try {
					e.setProductCount(productRepo.countByIsDeletedAndAdminStatusAndDesignerIdAndIsActive(false,
							"Approved", e.getdId(), true));
					e.setDesignerProfileEntity(
							designerProfileRepo.findBydesignerId(Long.parseLong(e.getdId().toString())).get());
				} catch (Exception o) {
					o.printStackTrace();
				}
				return e;
			});

			// .filter(e ->
			// e.getDesignerProfileEntity().getDesignerProfile().getDesignerCategory().equals("Pop"))

			return ResponseEntity.ok(map);

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/userDesignerPopList")
	public ResponseEntity<?> userDesignerPopList() {
		try {
			long count = databaseSeqRepo.findById(DesignerLoginEntity.SEQUENCE_NAME).get().getSeq();
			Random rd = new Random();
			List<DesignerLoginEntity> designerLoginEntity = new ArrayList<>();
			List<ProductMasterEntity2> productMasterData = new ArrayList<>();
			List<DesignerLoginEntity> findAll = designerLoginRepo
					.findByIsDeletedAndAndIsProfileCompletedAndAccountStatusAndDesignerCurrentStatus(false, true,
							"ACTIVE", "Online");			
			findAll.stream().forEach(data -> {
				List<ProductMasterEntity2> productByDesignerId = productRepo2.findByDesignerId(data.getdId());
				productMasterData.addAll(productByDesignerId);
			});			
			List<DesignerLoginEntity> collect = productMasterData.stream()
					.flatMap(d -> findAll.stream().filter(d1 -> d1.getdId().equals(d.getDesignerId().longValue()))).distinct()
					.collect(Collectors.toList());
			List<Integer> lst = new ArrayList<>();
			if (collect.size() <= 15) {
				designerLoginEntity = collect;
			} else {
				Boolean flag = true;
				while (flag) {
					int nextInt = rd.nextInt((int) count);
					for (DesignerLoginEntity obj : collect) {

						if (obj.getdId() == nextInt && !lst.contains(nextInt)) {
							lst.add(nextInt);
							designerLoginEntity.add(obj);
						}
						if (designerLoginEntity.size() > 14)
							flag = false;
					}
				}
			}
			Stream<DesignerLoginEntity> map = designerLoginEntity.stream().map(e -> {
						try {
							e.setProductCount(
									productRepo2.countByIsDeletedAndAdminStatusAndDesignerIdAndIsActiveAndSohNot(false,
											"Approved", e.getdId().intValue(), true, 0));
							org.json.simple.JSONObject countData = countData(e.getdId());
							String followerCount = countData.get("FollowersData").toString();
							e.setFollwerCount(Integer.parseInt(followerCount));
							e.setDesignerProfileEntity(
									designerProfileRepo.findBydesignerId(Long.parseLong(e.getdId().toString())).get());
						} catch (Exception o) {
							o.printStackTrace();
						}
						return e;
					})
					.filter(e -> e.getDesignerProfileEntity().getDesignerProfile().getDesignerCategory().equals("Pop"));
			return ResponseEntity.ok(map);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	// listing Logic
	public Map<String, Object> getDesignerProfDetails(int page, int limit, String sort, String sortName,
			Boolean isDeleted, String keyword, Optional<String> sortBy, String profileStatus) {
		try {
			int CountData = (int) designerLoginRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<DesignerLoginEntity> findAll = null;
			List<DesignerLoginEntity> dataForSubmittedAndCOMPLETED = designerLoginRepo
					.findByIsDeletedAndIsProfileCompleted(isDeleted, true, pagingSort).stream()

					.filter(e -> e.getProfileStatus().equals("COMPLETED") || e.getProfileStatus().equals("SUBMITTED"))
					.collect(Collectors.toList());
//			Page<DesignerLoginEntity> findByIsDeletedAndIsProfileCompletedAndProfileStatusOrProfileStatus = this.designerLoginRepo.findByIsDeletedAndIsProfileCompletedAndProfileStatusOrProfileStatus(isDeleted, true,
//					"COMPLETED", "SUBMITTED", pagingSort);

//					.filter(e -> e.getProfileStatus().equals("COMPLETED") || e.getProfileStatus().equals("SUBMITTED"))
//					.collect(Collectors.toList());

			if (!StringUtils.isEmpty(profileStatus)) {
				if (profileStatus.equals("changeRequest")) {
					findAll = designerLoginRepo.findByIsDeletedAndIsProfileCompletedAndProfileStatus(isDeleted, true,
							"SUBMITTED", pagingSort);
					;
				} else if (profileStatus.equals("COMPLETED")) {
					int startOfPage = pagingSort.getPageNumber() * pagingSort.getPageSize();
					int endOfPage = Math.min(startOfPage + pagingSort.getPageSize(),
							dataForSubmittedAndCOMPLETED.size());

					List<DesignerLoginEntity> subList = startOfPage >= endOfPage ? new ArrayList<>()
							: dataForSubmittedAndCOMPLETED.subList(startOfPage, endOfPage);

					findAll = new PageImpl<DesignerLoginEntity>(subList, pagingSort,
							dataForSubmittedAndCOMPLETED.size());
//					findAll=findByIsDeletedAndIsProfileCompletedAndProfileStatusOrProfileStatus;
					findAll = new PageImpl<DesignerLoginEntity>(subList, pagingSort,
							dataForSubmittedAndCOMPLETED.size());

				} else if (profileStatus.equals("SUBMITTED")) {
					findAll = designerLoginRepo.findByisDeletedAndIsProfileCompletedAndProfileStatus(isDeleted, false,
							"SUBMITTED", pagingSort);

				} else {
					findAll = designerLoginRepo.findByIsDeletedAndProfileStatusAndAccountStatus(isDeleted,
							profileStatus, "ACTIVE", pagingSort);
				}
			} else if (StringUtils.isEmpty(profileStatus) || StringUtils.isEmpty(keyword)) {
				findAll = designerLoginRepo.findDesignerisDeleted(true, pagingSort);
			} else {
				findAll = designerLoginRepo.SearchByDeletedAndProfileStatus(keyword, isDeleted, profileStatus,
						pagingSort);
			}

			List<Long> collect = findAll.getContent().stream()
					.filter(e -> keyword != "" ? e.getEmail().startsWith(keyword.toLowerCase()) : true)
					.map(e -> e.getdId()).collect(Collectors.toList());
			findAll = designerLoginRepo.findBydIdIn(collect, pagingSort);

			if (findAll.getSize() <= 0)
				throw new CustomException(MessageConstant.DESIGNER_ID_DOES_NOT_EXIST.getMessage());
			findAll.map(e -> {
				try {
					e.setDesignerProfileEntity(
							designerProfileRepo.findBydesignerId(Long.parseLong(e.getdId().toString())).get());
					e.getDesignerProfileEntity().setDesignerPersonalInfoEntity(
							designerPersonalInfoRepo.findByDesignerId(Long.parseLong(e.getdId().toString())).get());
				} catch (Exception o) {
					LOGGER.error("list" + o.getLocalizedMessage());
				}
				return e;
			});

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}
			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());
			response.put("waitingForApproval", designerLoginRepo
					.findByProfileStatusAndAccountStatusAndIsDeleted("waitForApprove", "ACTIVE", false).size());
			response.put("waitingForSubmit", designerLoginRepo
					.findByProfileStatusAndAccountStatusAndIsDeleted("APPROVE", "ACTIVE", false).size());
//			response.put("submitted", designerLoginRepo
//					.findByProfileStatusAndAccountStatusAndIsProfileCompleted("SUBMITTED", "ACTIVE", false).size());	
			response.put("submitted",
					designerLoginRepo.findByProfileStatusAndAccountStatusAndIsProfileCompletedAndIsDeleted("SUBMITTED",
							"ACTIVE", false, false).size());
			response.put("completed", (designerLoginRepo
					.findByProfileStatusAndAccountStatusAndIsDeleted("COMPLETED", "ACTIVE", false).size()
					+ designerLoginRepo.findByDeletedAndIsProfileCompletedAndProfileStatus(false, true, "SUBMITTED")
							.size()));
			response.put("rejected", designerLoginRepo
					.findByProfileStatusAndAccountStatusAndIsDeleted("REJECTED", "ACTIVE", false).size());
			response.put("deleted", designerLoginRepo.findByDeleted(true).size());
			response.put("changeRequest", designerLoginRepo
					.findByDeletedAndIsProfileCompletedAndProfileStatus(false, true, "SUBMITTED").size());
			response.put("saved",
					designerLoginRepo.findByProfileStatusAndAccountStatusAndIsDeleted("SAVED", "ACTIVE", false).size());

			return response;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/countData/{designerId}")
	public org.json.simple.JSONObject countData(@PathVariable Long designerId) {
		try {
			org.json.simple.JSONObject response = new org.json.simple.JSONObject();
			ResponseEntity<GlobalResponce> userData = restTemplate.getForEntity(
					USER_SERVICES + RestTemplateConstants.USER_FOLLOWER_COUNT + designerId, GlobalResponce.class);
//			ResponseEntity<GlobalResponce> userData = restTemplate.getForEntity(RestTemplateConstants.USER_FOLLOWER_COUNT_URL + designerId, GlobalResponce.class);

			if (!userData.getBody().equals(null)) {
				String followersData = userData.getBody().getMessage();
				response.put("FollowersData", followersData);
			}
			response.put("Products", productRepo2.countByIsDeletedAndAdminStatusAndDesignerIdAndIsActive(false,
					"Approved", designerId.intValue(), true));
			return response;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/getDesignerCategory")
	public List<Object> getDesignerCategory() {
		try {
			List<DesignerLoginEntity> designerProfileList = designerLoginRepo
					.findByIsDeletedAndisProfileCompletedAndAccountStatus(false, true, "ACTIVE");

			List<Long> list = new ArrayList<>();
			List<DesignerProfileEntity> designerProfileData = new ArrayList<>();

			for (DesignerLoginEntity entity : designerProfileList) {
				list.add(entity.getdId());
				designerProfileData = this.designerProfileRepo.findByDesignerIdIn(list);
				entity.setDesignerCategory(designerProfileData.get(0).getDesignerProfile().getDesignerCategory());
			}
			for (int i = 0; i < designerProfileData.size(); i++) {
				designerProfileList.get(i)
						.setDesignerCategory(designerProfileData.get(i).getDesignerProfile().getDesignerCategory());
			}

			List<Object> designercategories = new ArrayList<Object>();
			for (int i = 0; i < designerProfileList.size(); i++) {
				if (designerProfileList.get(i).getDesignerCategory() != null) {
					org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
					jsonObject.put("Name", designerProfileList.get(i).getDesignerCategory());
					if (!designercategories.contains(jsonObject)) {
						designercategories.add(jsonObject);
					}
				}
			}
			return designercategories;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/getDesignerDetails/{designerCategories}")
	public List<DesignerLoginEntity> getDesignerDetails(@RequestParam(defaultValue = "") String usermail,
			@RequestParam(defaultValue = "0") Double longitude, @RequestParam(defaultValue = "0") Double latitude,
			@PathVariable String designerCategories) {
		try {
			List<DesignerProfileEntity> mappedResults = new ArrayList<>();
			List<DesignerLoginEntity> blankList = new ArrayList<>();
			List<DesignerProfileEntity> collectDesigner = new ArrayList<>();
			NearQuery geoNear = NearQuery.near(longitude, latitude, Metrics.KILOMETERS)
					.maxDistance(new Distance(Long.parseLong(Max_Map_Distance), Metrics.KILOMETERS)).minDistance(0)
					.spherical(true);
			Aggregation agg = Aggregation.newAggregation(Aggregation.geoNear(geoNear, "coordinates"));
			AggregationResults<DesignerProfileEntity> result = mongoTemplate.aggregate(agg, DesignerProfileEntity.class,
					DesignerProfileEntity.class);
			mappedResults = result.getMappedResults();
			if (!designerCategories.equals("all")) {
				if (longitude != 0 && latitude != 0) {

					collectDesigner = mappedResults.stream()
							.filter(data -> data.getDesignerProfile().getDesignerCategory().equals(designerCategories))
							.collect(Collectors.toList());
				} else {
					collectDesigner = this.designerProfileRepo.findByDesignerCategory(designerCategories);

				}

				if (collectDesigner.size() <= 0) {
					return blankList;
				}
				String designerCategory = collectDesigner.get(0).getDesignerProfile().getDesignerCategory();
				if (designerCategory.isEmpty()) {
					return blankList;
				}

				List<Long> list = new ArrayList<>();
				for (DesignerProfileEntity dCategory : collectDesigner) {
					list.add(dCategory.getDesignerId());
				}

				List<DesignerLoginEntity> designerData = designerLoginRepo
						.findByIsDeletedAndAndIsProfileCompletedAndAccountStatusAndDesignerCurrentStatus(false, true,
								"ACTIVE", "Online");
				List<DesignerLoginEntity> collectData = collectDesigner.stream()
						.flatMap(e -> designerData.stream().filter(e1 -> e1.getdId().equals(e.getDesignerId())))
						.collect(Collectors.toList());

				if (collectData.size() > 0) {
					collectData.forEach(dRow -> {
						Query query2 = new Query();
						query2.addCriteria(Criteria.where("designerId").is(dRow.getdId())
								.andOperator(Criteria.where("designerCurrentStatus").is("Online")));
						DesignerProfileEntity designerProfileData = mongoOperations.findOne(query2,
								DesignerProfileEntity.class);
						dRow.setDesignerProfileEntity(designerProfileData);
						org.json.simple.JSONObject countData = this.countData(dRow.getdId());
						String productCount = countData.get("Products").toString();
						String followerCount = countData.get("FollowersData").toString();
						dRow.setProductCount(Integer.parseInt(productCount));
						dRow.setFollwerCount(Integer.parseInt(followerCount));
					});
				}
				if (usermail.equals("")) {
					return collectData;
				} else {
					UserDesignerEntity[] userDesignerEntity = restTemplate
							.getForEntity(USER_SERVICES + RestTemplateConstants.USER_DESIGNER_DETAILS + usermail,
									UserDesignerEntity[].class)
							.getBody();
					List<UserDesignerEntity> designerList = Arrays.asList(userDesignerEntity);
					collectData.stream().forEach(designer -> {
						if (designerList.stream().filter(dl -> dl.getDesignerId().equals(designer.getdId()))
								.count() > 0)
							designer.setIsFollowing(true);
						else
							designer.setIsFollowing(false);
					});
					return collectData;
				}
			} else {
				if (longitude != 0 && latitude != 0) {
					collectDesigner = mappedResults;
				} else {
					collectDesigner = this.designerProfileRepo
							.findByIsDeletedAndIsProfileCompletedAndDesignerCurrentStatusAndProfileStatus(false, true,
									"Online", "COMPLETED");
				}

				List<DesignerLoginEntity> designerData = designerLoginRepo
						.findByIsDeletedAndAndIsProfileCompletedAndAccountStatusAndDesignerCurrentStatus(false, true,
								"ACTIVE", "Online");

				List<DesignerLoginEntity> collectData = collectDesigner.stream()
						.flatMap(e -> designerData.stream().filter(e1 -> e1.getdId().equals(e.getDesignerId())))
						.collect(Collectors.toList());

				collectData.forEach(designerRow -> {
					Query query2 = new Query();
					query2.addCriteria(Criteria.where("designerId").is(designerRow.getdId())
							.andOperator(Criteria.where("designerCurrentStatus").is("Online")));
					DesignerProfileEntity designerProfileData = mongoOperations.findOne(query2,
							DesignerProfileEntity.class);
					designerRow.setDesignerProfileEntity(designerProfileData);
					org.json.simple.JSONObject countData = countData(designerRow.getdId());

					String productCount = countData.get("Products").toString();
					String followerCount = countData.get("FollowersData").toString();
					designerRow.setProductCount(Integer.parseInt(productCount));
					designerRow.setFollwerCount(Integer.parseInt(followerCount));
				});

				if (usermail.equals("")) {
					return collectData;
				} else {
					UserDesignerEntity[] userDesignerEntity = restTemplate
							.getForEntity(USER_SERVICES + RestTemplateConstants.USER_DESIGNER_DETAILS + usermail,
									UserDesignerEntity[].class)
							.getBody();
					List<UserDesignerEntity> designerList = Arrays.asList(userDesignerEntity);
					collectData.stream().forEach(designer -> {
						if (designerList.stream().filter(dl -> dl.getDesignerId().equals(designer.getdId()))
								.count() > 0)
							designer.setIsFollowing(true);
						else
							designer.setIsFollowing(false);
					});
					return collectData;
				}
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/designerIdList")
	public List<DesignerLoginEntity> getDesignerIdList() {
		try {
			return designerLoginRepo.findByIsDeletedAndProfileStatusAndAccountStatus(false, "COMPLETED", "ACTIVE");
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/designerStatusInformation")
	public Map<String, Object> getTotalActiveDesigner() {

		try {
			LOGGER.info("Inside - ProductController.getAllProductDetails()");
			return this.getDesignerInformation();
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	private Map<String, Object> getDesignerInformation() {
		try {
			LOGGER.info("Inside - ProductController.getDesignerInformation()");
			Pageable pagingSort = PageRequest.of(0, 10);
			Page<DesignerLoginEntity> findAllCompleted = designerLoginRepo.findDesignerProfileStatus("COMPLETED",
					pagingSort);
			Page<DesignerLoginEntity> findAllApproved = designerLoginRepo.findDesignerProfileStatus("APPROVE",
					pagingSort);
			Page<DesignerLoginEntity> findAllRejected = designerLoginRepo.findDesignerProfileStatus("REJECTED",
					pagingSort);
			Page<DesignerLoginEntity> findAllSubmitted = designerLoginRepo.findDesignerProfileStatus("SUBMITTED",
					pagingSort);
			Page<DesignerLoginEntity> findAllWaitForApprove = designerLoginRepo
					.findDesignerProfileStatus("waitForApprove", pagingSort);
			Page<DesignerLoginEntity> findAllDeleted = designerLoginRepo.findDesignerisDeleted(true, pagingSort);
			Map<String, Object> response = new HashMap<>();
			response.put("Completed", findAllCompleted.getTotalElements());
			response.put("Approve", findAllApproved.getNumberOfElements());
			response.put("Rejected", findAllRejected.getNumberOfElements());
			response.put("Submitted", findAllSubmitted.getNumberOfElements());
			response.put("WaitForApprove", findAllWaitForApprove.getNumberOfElements());
			response.put("Deleted", findAllDeleted.getNumberOfElements());
			return response;

		} catch (Exception e) {

			throw new CustomException(e.getMessage());

		}
	}

	@PutMapping("/designerCurrentStatus/{status}")
	public GlobalResponce changeDesignerStatus(@RequestHeader("Authorization") String token,
			@PathVariable String status) {
		try {
			LOGGER.info("Inside changeDesignerStatus");

			Optional<DesignerLoginEntity> findByEmail = designerLoginRepo
					.findByEmail(jwtConfig.extractUsername(token.substring(7)));
			DesignerLoginEntity designerLoginEntity = findByEmail.get();
			Optional<DesignerProfileEntity> findBydesignerId = designerProfileRepo
					.findBydesignerId(findByEmail.get().getdId());

			if (findBydesignerId.orElse(null) != null) {
				DesignerProfileEntity designerProfileEntity2 = findBydesignerId.get();
				designerProfileEntity2.setDesignerCurrentStatus(status);
				designerProfileRepo.save(designerProfileEntity2);
			}
			if (findByEmail.isPresent()) {
				designerLoginEntity.setDesignerCurrentStatus(status);
				designerLoginRepo.save(designerLoginEntity);
			} else {
				throw new CustomException(MessageConstant.USER_NOT_FOUND.getMessage());
			}
			if (status.equals("Online")) {
				return new GlobalResponce(MessageConstant.SUCCESS.getMessage(),
						MessageConstant.STATUS_ACTIVE.getMessage(), 200);
			} else {
				return new GlobalResponce(MessageConstant.SUCCESS.getMessage(),
						MessageConstant.STATUS_INACTIVE.getMessage(), 200);
			}
		} catch (RuntimeException e) {
			throw new CustomException("Token Expired");
		} catch (Exception ex) {
			throw new CustomException(ex.getMessage());
		}
	}

	@PostMapping("/profilePicUpdate")
	public GlobalResponce imageUpload(@RequestBody ProfileImage profileimage) {
		try {
			Long designerId = profileimage.getDesignerId();

			if (designerId != null) {
				DesignerProfileEntity findBydesignerId = designerProfileRepo.findBydesignerId(designerId).get();
				DesignerProfileEntity designerProfileEntity = new DesignerProfileEntity();
				DesignerProfile designerProfile = new DesignerProfile();

				designerProfileEntity.setDesignerId(profileimage.getDesignerId());
				designerProfileEntity.setBoutiqueProfile(findBydesignerId.getBoutiqueProfile());
				designerProfileEntity.setDesignerPersonalInfoEntity(findBydesignerId.getDesignerPersonalInfoEntity());
				designerProfileEntity.setSocialProfile(findBydesignerId.getSocialProfile());
				designerProfileEntity.setAccountStatus(findBydesignerId.getAccountStatus());
				designerProfileEntity.setDesignerLevel(findBydesignerId.getDesignerLevel());
				designerProfileEntity.setFollowerCount(findBydesignerId.getFollowerCount());
				designerProfileEntity.setId(findBydesignerId.getId());
				designerProfileEntity.setDesignerName(findBydesignerId.getDesignerName());
				designerProfileEntity.setIsDeleted(findBydesignerId.getIsDeleted());
				designerProfileEntity.setMenChartData(findBydesignerId.getMenChartData());
				designerProfileEntity.setWomenChartData(findBydesignerId.getWomenChartData());
				designerProfileEntity.setProductCount(findBydesignerId.getProductCount());
				designerProfileEntity.setProfileStatus(findBydesignerId.getProfileStatus());
				designerProfileEntity.setDesignerCurrentStatus(findBydesignerId.getDesignerCurrentStatus());
				designerProfileEntity.setIsProfileCompleted(findBydesignerId.getIsProfileCompleted());
				designerProfileEntity.setUid(findBydesignerId.getUid());

				designerProfile.setAltMobileNo(findBydesignerId.getDesignerProfile().getAltMobileNo());
				designerProfile.setCity(findBydesignerId.getDesignerProfile().getCity());
				designerProfile.setCountry(findBydesignerId.getDesignerProfile().getCountry());
				designerProfile.setDesignerCategory(findBydesignerId.getDesignerProfile().getDesignerCategory());
				designerProfile.setDigitalSignature(findBydesignerId.getDesignerProfile().getDigitalSignature());
				designerProfile.setDisplayName(findBydesignerId.getDesignerProfile().getDisplayName());
				designerProfile.setDob(findBydesignerId.getDesignerProfile().getDob());
				designerProfile.setEmail(findBydesignerId.getDesignerProfile().getEmail());
				designerProfile.setFirstName1(findBydesignerId.getDesignerProfile().getFirstName1());
				designerProfile.setLastName1(findBydesignerId.getDesignerProfile().getLastName1());
				designerProfile.setFirstName2(findBydesignerId.getDesignerProfile().getFirstName2());
				designerProfile.setLastName2(findBydesignerId.getDesignerProfile().getLastName2());
				designerProfile.setGender(findBydesignerId.getDesignerProfile().getGender());
				designerProfile.setMobileNo(findBydesignerId.getDesignerProfile().getMobileNo());
				designerProfile.setPassword(findBydesignerId.getDesignerProfile().getPassword());
				designerProfile.setPinCode(findBydesignerId.getDesignerProfile().getPinCode());
				designerProfile.setProfilePic(profileimage.getImage());
				designerProfile.setState(findBydesignerId.getDesignerProfile().getState());
				designerProfile.setUid(findBydesignerId.getDesignerProfile().getUid());

				designerProfileEntity.setDesignerProfile(designerProfile);

				designerProfileRepo.save(designerProfileEntity);

				return new GlobalResponce(MessageConstant.SUCCESS.getMessage(),
						MessageConstant.PROFILE_IMAGE_UPDATED.getMessage(), 200);
			} else {
				throw new CustomException(MessageConstant.DESIGNER_ID_DOES_NOT_EXIST.getMessage());
			}

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/getProfileImage/{designerId}")
	public Map<String, String> getProfileImage(@PathVariable Long designerId) {
		try {
			DesignerProfileEntity findByDesignerId = designerProfileRepo.findBydesignerId(designerId).get();
			Map<String, String> map = new HashMap<>();
			map.put("profilePic", findByDesignerId.getDesignerProfile().getProfilePic());
			return map;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@PutMapping("/designerProfileDelete")
	public GlobalResponce designerProfileDelete(@RequestHeader("Authorization") String token,
			@RequestParam String designerEmail) {
		try {
			DesignerLoginEntity designerLoginEntity = designerLoginRepo.findByEmail(designerEmail).get();
			if (designerLoginEntity.getIsDeleted()) {
				return new GlobalResponce("Error", "Designer is already deleted", 400);
			} else {
				designerLoginEntity.setIsDeleted(true);
				designerLoginRepo.save(designerLoginEntity);
				return new GlobalResponce("Success", "Designer is successfully deleted", 200);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@GetMapping("/getDesignerToken")
	public ResponseEntity<?> getDesignerToken(@RequestHeader("Authorization") String token) {
		Optional<DesignerLoginEntity> findByEmail = Optional.empty();

		try {
			if (!token.isEmpty() && token != "") {
				findByEmail = designerLoginRepo.findByEmail(jwtConfig.extractUsername(token.substring(7)));

				if (findByEmail.orElse(null) != null && findByEmail.orElse(null) != null) {
					Optional<DesignerProfileEntity> findBydesignerId = designerProfileRepo
							.findBydesignerId(findByEmail.get().getdId());
					findByEmail.get().setDesignerProfileEntity(findBydesignerId.get());
					if (findBydesignerId.orElse(null) != null) {
						return new ResponseEntity<>(findByEmail, HttpStatus.OK);
					}
				}
			}
			return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/postContacts")

	public GlobalResponce postContactsDetails(@RequestParam Long designerId,
			@RequestBody DesignerContactEntity designerContactEntity) {

		try {

//			 

			DesignerProfileEntity designerProfileEntity = designerProfileRepo.findBydesignerId(designerId).get();
			ResponseEntity<RazorpayX> postForEntity = restTemplate.postForEntity(
					ADMIN_SERVICE + RestTemplateConstants.ADMIN_ADD_CONTACTS + "?designerId=" + designerId,
					designerContactEntity, RazorpayX.class);
			RazorpayX body = postForEntity.getBody();
			designerProfileEntity.setRazorpayX(body);

			designerProfileRepo.save(designerProfileEntity);
			return new GlobalResponce(MessageConstant.SUCCESS.getMessage(), 200);

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@PostMapping("/postFundsAccount")

	public GlobalResponce postFundAccountDetails(@RequestParam Long designerId,
			@RequestBody DesignerFundsAccount designerFundsAccount) {
		try {

			DesignerProfileEntity designerProfileEntity = designerProfileRepo.findBydesignerId(designerId).get();
			ResponseEntity<RazorpayX> postForEntity = restTemplate.postForEntity(
					ADMIN_SERVICE + RestTemplateConstants.ADMIN_ADD_FUNDS_ACCOUNT + "?designerId=" + designerId,
					designerFundsAccount, RazorpayX.class);
			DesignerProfileEntity profileEntity = new DesignerProfileEntity();
			List<String> contacts = designerProfileEntity.getRazorpayX().getContacts();
			RazorpayX body = postForEntity.getBody();
			RazorpayX razorpayX = new RazorpayX();

			designerProfileEntity.setRazorpayX(body);

			designerProfileRepo.save(designerProfileEntity);

			return new GlobalResponce(MessageConstant.SUCCESS.getMessage(), 200);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

//	@PostMapping("/addPayOutsData")
//	public ResponseEntity<?> addPayOutDetails(@RequestParam Long designerId, @RequestBody PayOutDTO payOutDTO){
//		try {
//			//Long designerId = payOutDTO.getDesignerId();
//			DesignerProfileEntity designerProfileEntity = designerProfileRepo.findBydesignerId(designerId).get();
//			RazorpayX razorPayData = restTemplate .postForEntity
//			        (ADMIN_SERVICE+RestTemplateConstants.ADMIN_ADD_PAYOUT_DATA + "?designerId="+ designerId, payOutDTO, RazorpayX.class ).getBody();
//			designerProfileEntity.setRazorpayX(razorPayData);
//			designerProfileRepo.save(designerProfileEntity);
//			return new ResponseEntity<>("added successfully", HttpStatus.OK);
//		}catch (Exception e) {
//			throw new CustomException(e.getMessage());
//		}
//	}

//	@GetMapping("/getDesignerByArea")
//	public Map<String, Object> getDesignerByArea(@RequestParam(defaultValue = "0") int page,
//			@RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "DESC") String sort,
//			@RequestParam(defaultValue = "designerId") String sortName,
//			@RequestParam(defaultValue = "false") Boolean isDeleted, @RequestParam Optional<String> sortBy,
//			@RequestParam(defaultValue = "") double longitude, @RequestParam(defaultValue = "") double latitude) {
//		try {
//			int CountData = (int) designerProfileRepo.count();
//			Pageable pagingSort = null;
//			if (limit == 0) {
//				limit = CountData;
//			}
//			if (sort.equals("ASC")) {
//				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
//			} else {
//				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
//			}
//
//			Page<DesignerProfileEntity> findAll = null;
//			NearQuery geoNear = NearQuery.near(longitude, latitude, Metrics.KILOMETERS)
//					.maxDistance(new Distance(Long.parseLong(Max_Map_Distance), Metrics.KILOMETERS)).minDistance(0)
//					.spherical(true);
//
//			Aggregation agg = Aggregation.newAggregation(Aggregation.geoNear(geoNear, "coordinates"));
//			AggregationResults<DesignerProfileEntity> result = mongoTemplate.aggregate(agg, DesignerProfileEntity.class,
//					DesignerProfileEntity.class);
//			List<DesignerProfileEntity> mappedResults = result.getMappedResults();
////			List<ProductMasterEntity2> products = new ArrayList<>();
////			mappedResults.forEach(e -> {
////				List<ProductMasterEntity2> filterProduct = this.productRepo2
////						.findByDesignerId(Integer.parseInt(e.getDesignerId().toString())).stream()
////						.filter(prod -> prod.getSoh() != 0).filter(prod -> prod.getIsActive().equals(true))
////						.filter(prod -> prod.getIsDeleted().equals(false))
////						.filter(prod -> !prod.getAdminStatus().equals("Rejected")).collect(Collectors.toList());
////				products.addAll(filterProduct);
////			});
//
//			int startOfPage = pagingSort.getPageNumber() * pagingSort.getPageSize();
//			int endOfPage = Math.min(startOfPage + pagingSort.getPageSize(), mappedResults.size());
//
//			List<DesignerProfileEntity> subList = startOfPage >= endOfPage ? new ArrayList<>()
//					: mappedResults.subList(startOfPage, endOfPage);
//			findAll = new PageImpl<DesignerProfileEntity>(subList, pagingSort, mappedResults.size());
//
//			int totalPage = findAll.getTotalPages() - 1;
//			if (totalPage < 0) {
//				totalPage = 0;
//			}
//
//			Map<String, Object> response = new HashMap<>();
//			response.put("data", findAll.getContent());
//			response.put("currentPage", findAll.getNumber());
//			response.put("total", findAll.getTotalElements());
//			response.put("totalPage", totalPage);
//			response.put("perPage", findAll.getSize());
//			response.put("perPageElement", findAll.getNumberOfElements());
//			return response;
//
//		} catch (Exception e) {
//			throw new CustomException(e.getMessage());
//		}
//	}

//	@GetMapping("/getGeoAddress")
//	public ResponseEntity<?> getGoogleAddress(@RequestHeader("Authorization") String token,
//			@RequestParam(value = "address", required = false) String address) {
//
//		Optional<DesignerLoginEntity> findByEmail = Optional.empty();
//		ResponseEntity<Object> getAddress = null;
//		try {
//			if (!token.isEmpty() && token != "") {
//
//				findByEmail = designerLoginRepo.findByEmail(jwtConfig.extractUsername(token.substring(7)));
//
//				if (findByEmail.isPresent()) {
//					getAddress = restTemplate.getForEntity(RestTemplateConstants.GOOGLE_GEOCODING_URL + "address="
//							+ address + "&key=" + GOOGLE_MAP_APIKEY, Object.class);
//
//					return new ResponseEntity<>(getAddress.getBody(), HttpStatus.OK);
//				}
//			}
//			return new ResponseEntity<>("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
//		} catch (Exception e) {
//			return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//		}
//	}

}
