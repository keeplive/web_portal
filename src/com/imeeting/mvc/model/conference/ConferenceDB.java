package com.imeeting.mvc.model.conference;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.imeeting.constants.AttendeeConstants;
import com.imeeting.constants.ConferenceConstants;
import com.imeeting.mvc.model.conference.attendee.AttendeeBean;
import com.imeeting.mvc.model.conference.attendee.AttendeeModel;

public class ConferenceDB {
	private static Log log = LogFactory.getLog(ConferenceDB.class);

	private JdbcTemplate jdbc;

	public enum ConferenceStatus {
		OPEN, CLOSE
	};

	public enum UserConfStatus {
		VISIABLE, HIDDEN
	};

	public void setDataSource(DataSource ds) {
		jdbc = new JdbcTemplate(ds);
	}

	public void saveConference(ConferenceModel conference, String title)
			throws DataAccessException {
		insert(conference.getConferenceId());
		if (title == null || title.equals("")) {
			title = "群聊号: " + conference.getConferenceId();
		}
		editConferenceTitle(conference.getConferenceId(), title);
		Collection<AttendeeModel> attendeeCollection = conference
				.getAllAttendees();
		saveAttendeeBeans(conference.getConferenceId(), attendeeCollection);
	}

	public void saveAttendeeBeans(String conferenceId,
			Collection<AttendeeModel> attendeeCollection)
			throws DataAccessException {
		String sql = "INSERT INTO im_attendee(conferenceId, username, nickname) VALUES(?,?,?)";
		List<Object[]> params = new ArrayList<Object[]>();
		for (AttendeeModel attendee : attendeeCollection) {
			params.add(new Object[] { conferenceId, attendee.getUsername(), attendee.getNickname() });
		}
		jdbc.batchUpdate(sql, params);
	}

	public void saveAttendee(String conferenceId, AttendeeModel attendee) {
		Collection<AttendeeModel> attendees = new ArrayList<AttendeeModel>();
		attendees.add(attendee);
		saveAttendeeBeans(conferenceId, attendees);
	}
	
	public void removeAttendee(String conferenceId, AttendeeModel attendee) {
		String sql = "DELETE FROM im_attendee WHERE conferenceId = ? AND username = ?";
		jdbc.update(sql, conferenceId, attendee.getUsername());
	}

	public int insert(String conferenceId) throws DataAccessException {
		return jdbc.update(
				"INSERT INTO im_conference(conferenceId) VALUES (?)",
				conferenceId);
	}

	public int close(String conferenceId) throws DataAccessException {
		return setStatus(conferenceId, ConferenceStatus.CLOSE);
	}

	private int setStatus(String conferenceId, ConferenceStatus status)
			throws DataAccessException {
		return jdbc.update(
				"UPDATE im_conference SET status=? WHERE conferenceId=?",
				status.name(), conferenceId);
	}

	/**
	 * 获取所有的会议
	 * 
	 * @param username
	 * @return
	 */
	public int getAllConferenceCount(String username) {
		String sql = "SELECT COUNT(c.conferenceId) FROM im_conference AS c "
				+ "INNER JOIN im_attendee AS a ON c.conferenceId=a.conferenceId "
				+ "AND a.username=? ORDER BY c.created DESC";
		return jdbc.queryForInt(sql, username);
	}

	/**
	 * 获取VISIBLE状态的会议
	 * 
	 * @param username
	 * @return
	 * @throws DataAccessException
	 */
	public int getConferenceTotalCount(String username)
			throws DataAccessException {
		// query the total count of conference list related to username
		String sql = "SELECT count(c.conferenceId) "
				+ "FROM im_conference AS c INNER JOIN im_attendee AS a "
				+ "ON c.conferenceId = a.conferenceId AND a.username = ? AND a.status = ? "
				+ "ORDER BY c.created DESC";
		return jdbc.queryForInt(sql, username, UserConfStatus.VISIABLE.name());
	}

	public List<ConferenceBean> getConferenceList(String userName, int offset,
			int pageSize) {
		String sql = "SELECT c.conferenceId AS id, UNIX_TIMESTAMP(c.created) AS created, c.status, c.title "
				+ "FROM im_conference AS c INNER JOIN im_attendee AS a "
				+ "ON c.conferenceId = a.conferenceId AND a.username = ? "
				+ "ORDER BY c.created DESC LIMIT ?, ?";

		int startIndex = (offset - 1) * pageSize;
		List<Map<String, Object>> confResultList = jdbc.queryForList(sql,
				userName, startIndex, pageSize);
		ArrayList<ConferenceBean> beanList = new ArrayList<ConferenceBean>();
		for (Map<String, Object> c : confResultList) {
			ConferenceBean bean = new ConferenceBean();
			bean.setId((String) c.get("id"));
			bean.setTitle((String) c.get("title"));
			bean.setCreatedTimeStamp((Long) c.get("created") * 1000);
			beanList.add(bean);
		}
		return beanList;
	}

	public JSONArray getConferenceWithAttendeesList(String userName,
			int offset, int pageSize) throws DataAccessException {
		// query conference list related to username
		String sql = "SELECT c.conferenceId AS id, UNIX_TIMESTAMP(c.created) AS created, c.status, c.title "
				+ "FROM im_conference AS c INNER JOIN im_attendee AS a "
				+ "ON c.conferenceId = a.conferenceId AND a.username = ? AND a.status = ? "
				+ "ORDER BY c.created DESC LIMIT ?, ?";

		int startIndex = (offset - 1) * pageSize;
		List<Map<String, Object>> confResultList = jdbc.queryForList(sql,
				userName, UserConfStatus.VISIABLE.name(), startIndex, pageSize);

		log.debug("confResultList size: " + confResultList.size());

		if (confResultList.size() <= 0) {
			return new JSONArray();
		}

		StringBuffer confIds = new StringBuffer();
		confIds.append('(');

		HashMap<String, JSONObject> confInfoMap = new HashMap<String, JSONObject>();

		for (Map<String, Object> confMap : confResultList) {
			String confId = (String) confMap.get("id");
			Long createdTime = (Long) confMap.get("created");
			String status = (String) confMap.get("status");
			String title = (String) confMap.get("title");
			log.debug("conferenceId: " + confId);
			log.debug("created time: " + createdTime.longValue());
			log.debug("status: " + status);
			log.debug("title: " + title);

			JSONObject conference = new JSONObject();
			try {
				conference.put(ConferenceConstants.conferenceId.name(), confId);
				conference.put(ConferenceConstants.created_time.name(),
						createdTime);
				conference.put(ConferenceConstants.status.name(), status);
				conference.put(ConferenceConstants.title.name(), title);
				JSONArray attendees = new JSONArray();
				conference.put(ConferenceConstants.attendees.name(), attendees);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			confInfoMap.put(confId, conference);

			confIds.append("'" + confId + "'").append(',');
		}
		if (confIds.lastIndexOf(",") == confIds.length() - 1) {
			confIds.deleteCharAt(confIds.length() - 1);
		}
		confIds.append(')');

		// query the attendees in each conference
		sql = "SELECT conferenceId AS id, username, nickname " + "FROM im_attendee "
				+ "WHERE conferenceId IN " + confIds.toString();
		List<Map<String, Object>> attendeeList = jdbc.queryForList(sql);

		for (Map<String, Object> attendeeMap : attendeeList) {
			String confId = (String) attendeeMap.get("id");
			String attendeeUserName = (String) attendeeMap
					.get(AttendeeConstants.username.name());
			String attendeeNickname = (String) attendeeMap.get(AttendeeConstants.nickname.name());
			JSONObject conference = confInfoMap.get(confId);
			try {
				JSONArray attendees = conference
						.getJSONArray(ConferenceConstants.attendees.name());
				JSONObject attendeeJson = new JSONObject();
				attendeeJson.put(AttendeeConstants.username.name(), attendeeUserName);
				attendeeJson.put(AttendeeConstants.nickname.name(), attendeeNickname);
				attendees.put(attendeeJson);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// convert conference info map to json array
		JSONArray confJSONArray = new JSONArray();
		for (Map<String, Object> confInfo : confResultList) {
			String conferenceId = (String) confInfo.get("id");
			JSONObject conference = confInfoMap.get(conferenceId);
			confJSONArray.put(conference);
		}
		return confJSONArray;
	}
	
	// to be compatible with old apps
	@Deprecated
	public JSONArray getConferenceWithAttendeesListOld(String userName,
			int offset, int pageSize) throws DataAccessException {
		// query conference list related to username
		String sql = "SELECT c.conferenceId AS id, UNIX_TIMESTAMP(c.created) AS created, c.status, c.title "
				+ "FROM im_conference AS c INNER JOIN im_attendee AS a "
				+ "ON c.conferenceId = a.conferenceId AND a.username = ? AND a.status = ? "
				+ "ORDER BY c.created DESC LIMIT ?, ?";

		int startIndex = (offset - 1) * pageSize;
		List<Map<String, Object>> confResultList = jdbc.queryForList(sql,
				userName, UserConfStatus.VISIABLE.name(), startIndex, pageSize);

		log.debug("confResultList size: " + confResultList.size());

		if (confResultList.size() <= 0) {
			return new JSONArray();
		}

		StringBuffer confIds = new StringBuffer();
		confIds.append('(');

		HashMap<String, JSONObject> confInfoMap = new HashMap<String, JSONObject>();

		for (Map<String, Object> confMap : confResultList) {
			String confId = (String) confMap.get("id");
			Long createdTime = (Long) confMap.get("created");
			String status = (String) confMap.get("status");
			String title = (String) confMap.get("title");
			log.debug("conferenceId: " + confId);
			log.debug("created time: " + createdTime.longValue());
			log.debug("status: " + status);
			log.debug("title: " + title);

			JSONObject conference = new JSONObject();
			try {
				conference.put(ConferenceConstants.conferenceId.name(), confId);
				conference.put(ConferenceConstants.created_time.name(),
						createdTime);
				conference.put(ConferenceConstants.status.name(), status);
				conference.put(ConferenceConstants.title.name(), title);
				JSONArray attendees = new JSONArray();
				conference.put(ConferenceConstants.attendees.name(), attendees);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			confInfoMap.put(confId, conference);

			confIds.append("'" + confId + "'").append(',');
		}
		if (confIds.lastIndexOf(",") == confIds.length() - 1) {
			confIds.deleteCharAt(confIds.length() - 1);
		}
		confIds.append(')');

		// query the attendees in each conference
		sql = "SELECT conferenceId AS id, username, nickname " + "FROM im_attendee "
				+ "WHERE conferenceId IN " + confIds.toString();
		List<Map<String, Object>> attendeeList = jdbc.queryForList(sql);

		for (Map<String, Object> attendeeMap : attendeeList) {
			String confId = (String) attendeeMap.get("id");
			String attendeeUserName = (String) attendeeMap
					.get(AttendeeConstants.username.name());
			JSONObject conference = confInfoMap.get(confId);
			try {
				JSONArray attendees = conference
						.getJSONArray(ConferenceConstants.attendees.name());
				attendees.put(attendeeUserName);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// convert conference info map to json array
		JSONArray confJSONArray = new JSONArray();
		for (Map<String, Object> confInfo : confResultList) {
			String conferenceId = (String) confInfo.get("id");
			JSONObject conference = confInfoMap.get(conferenceId);
			confJSONArray.put(conference);
		}
		return confJSONArray;
	}

	public int makeConferenceVisibleForEachAttendee(String conferenceId)
			throws DataAccessException {
		String sql = "UPDATE im_attendee SET status = ? WHERE conferenceId = ? ";
		return jdbc.update(sql, UserConfStatus.VISIABLE.name(), conferenceId);
	}

	public int hideConference(String conferenceId, String userName)
			throws DataAccessException {
		String sql = "UPDATE im_attendee SET status = ? WHERE conferenceId = ? AND username = ?";
		return jdbc.update(sql, UserConfStatus.HIDDEN.name(), conferenceId,
				userName);
	}

	/**
	 * get attendees from conference
	 * 
	 * @param conferenceId
	 * @return List<Map<String, Object>>
	 * @throws SQLException
	 */
	public List<AttendeeBean> getConferenceAttendees(List<String> confIdList)
			throws DataAccessException {
		String confIdString = "";
		for (int i = 0; i < confIdList.size(); i++) {
			confIdString += confIdList.get(i);
			if (i + 1 < confIdList.size()) {
				confIdString += ", ";
			}
		}
		return jdbc.query("SELECT conferenceId, username, nickname FROM im_attendee "
				+ "WHERE conferenceId IN (" + confIdString + ")",
				new RowMapper<AttendeeBean>() {
					@Override
					public AttendeeBean mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						AttendeeBean bean = new AttendeeBean();
						bean.setConferenceId(rs.getString("conferenceId"));
						bean.setUserName(rs.getString("username"));
						bean.setNickName(rs.getString("nickname"));
						return bean;
					}
				});
	}

	public int editConferenceTitle(String conferenceId, String title)
			throws DataAccessException {
		String sql = "UPDATE im_conference SET title = ? WHERE conferenceId = ?";
		return jdbc.update(sql, title, conferenceId);
	}

	/**
	 * update status of specified conference
	 * 
	 * @param conferenceId
	 * @param status
	 * @return rows
	 * @throws SQLException
	 */
	public int updateStatus(String conferenceId, ConferenceStatus status)
			throws DataAccessException {
		log.debug("updateStatus - " + " conferenceId: " + conferenceId);
		return setStatus(conferenceId, status);
	}

	public List<String> getTokens(String userNames) {
		String sql = "SELECT token FROM im_token WHERE username IN "
				+ userNames;
		List<String> tokens = jdbc.queryForList(sql, String.class);
		return tokens;
	}
}
