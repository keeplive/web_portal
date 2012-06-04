package com.richitec.ucenter.model;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import com.richitec.db.DBHelper;
import com.richitec.util.MD5Util;
import com.richitec.util.RandomString;
import com.richitec.util.SMSClient;
import com.richitec.util.ValidatePattern;

public class User {
	private static Log log = LogFactory.getLog(User.class);
	public static final String SESSION_BEAN = "userBean";
	public static final String PASSWORD_STR = "huuguanghui";

	/**
	 * 获得手机验证�?
	 * 
	 * @param session
	 * @param phone
	 * @param phoneCode
	 * @return
	 */
	public static String getPhoneCode(HttpSession session, String phone) {
		String result = "0";
		String phoneCode = RandomString.validateCode();
		log.info("phone code: " + phoneCode);
		try {
			session.setAttribute("phonenumber", phone);
			session.setAttribute("phonecode", phoneCode);
			SMSClient.getInstance().sendValidateCode(phone, phoneCode);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 注册
	 * 
	 * @param session
	 * @param phone
	 * @param password
	 * @param password1
	 * @param code
	 * @return
	 */
	public static String regUser(String phone, String password, String password1) {
		String result = checkRegisterUser(phone, password, password1);
		if (result.equals("0")) {
			String userkey = MD5Util.md5(phone + password);
			String sql = "INSERT INTO im_user(username, password, userkey) VALUE (?,?,?)";
			Object[] params = new Object[] { Long.parseLong(phone),
					MD5Util.md5(password), userkey };
			try {
				int resultCount = DBHelper.getInstance().update(sql, params);
				result = resultCount > 0 ? "0" : "1001";
			} catch (SQLException e) {
				e.printStackTrace();
				result = "1001";
			}
		}
		return result;
	}

	/**
	 * 登录
	 * 
	 * @param session
	 * @param loginName
	 * @param loginPwd
	 * @return
	 */
	public static String login(HttpSession session, String loginName,
			String loginPwd) {
		String result = "";
		String sql = "SELECT password FROM im_user WHERE username = ?";
		Object[] params = new Object[] { loginName };
		try {
			String password = DBHelper.getInstance().scalar(sql, params);
			if (loginPwd.equals(password)) {
				result = "0";
			} else {
				result = "1";
			}
		} catch (SQLException e) {
			e.printStackTrace();
			result = "1001";
		}
		return result;
	}

	/**
	 * record the device info of login user
	 * 
	 * @param username
	 * @param brand
	 * @param model
	 * @param release
	 * @param sdk
	 * @param width
	 * @param height
	 */
	public static void recodeDeviceInfo(String username, String brand,
			String model, String release, String sdk, String width,
			String height) {
		log.info("record device info - username:  " + username + " brand: "
				+ brand);
		DBHelper dh = DBHelper.getInstance();

		String sql = "SELECT count(username) FROM fy_device_info WHERE username = ?";
		Object[] params = new Object[] { username };
		try {
			int count = dh.count(sql, params);
			if (count > 0) {
				// update device info
				sql = "UPDATE fy_device_info SET brand=?, model=?, release_ver=?, sdk=?, width=?, height=? WHERE username = ?";
				params = new Object[] { brand, model, release, sdk, width,
						height, username };
				dh.update(sql, params);
			} else {
				// insert new device info
				sql = "INSERT INTO fy_device_info VALUES(?,?,?,?,?,?,?)";
				params = new Object[] { username, brand, model, release, sdk,
						width, height };
				dh.update(sql, params);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static String checkPhoneCode(HttpSession session, String code) {
		if (code.equals("")) {
			return "1"; // 验证码必�?
		} else if (!code.equals(session.getAttribute("phonecode"))) {
			return "2"; // 验证码错�?
		} else {
			return "0";
		}
	}

	/**
	 * 判断用户注册信息是否正确
	 * 
	 * @param session
	 * @param phone
	 * @param password
	 * @param password1
	 * @param code
	 * @return
	 */
	public static String checkRegisterUser(String phone, String password,
			String password1) {
		if (phone.equals("")) {
			return "1"; // 手机号码必填
		} else if (!ValidatePattern.isValidMobilePhone(phone)) {
			return "2"; // 手机号码格式错误
		} else if (!isExistsLoginName(phone).equals("0")) {
			return "3"; // 手机号码已存�?
		} else if (password.equals("")) {
			return "4"; // 密码必填
		} else if (!password.equals(password1)) {
			return "5"; // 两次密码输入不一�?
		} else {
			return "0";
		}

	}

	/**
	 * 判断手机号码是否正确
	 * 
	 * @param phone
	 * @return
	 */
	public static String checkRegisterPhone(String phone) {
		if (phone.equals("")) {
			return "1"; // 手机号码必填
		} else if (!ValidatePattern.isValidMobilePhone(phone)) {
			return "2"; // 手机号码格式错误
		} else if (!isExistsLoginName(phone).equals("0")) {
			return "3"; // 手机号码已存�?
		} else {
			return "0";
		}
	}

	/**
	 * 判断该用户名是否存在
	 * 
	 * @param loginName
	 * @return
	 */
	private static String isExistsLoginName(String loginName) {
		String sql = "SELECT count(username) FROM im_user WHERE username = ?";
		Object[] params = new Object[] { loginName };
		int count = 0;
		try {
			count = DBHelper.getInstance().count(sql, params);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return count == 0 ? "0" : "1"; // 0:不存�?1：存�?
	}

	/**
	 * 获得userkey
	 * 
	 * @param phone
	 * @return
	 */
	public static String getUserKey(String phone) {
		String userkey = null;
		String sql = "SELECT userkey FROM im_user WHERE username = ?";
		Object[] params = new Object[] { phone };
		try {
			userkey = DBHelper.getInstance().scalar(sql, params);
		} catch (SQLException e) {
			e.printStackTrace();
			userkey = null;
		}
		return userkey;
	}
	
}