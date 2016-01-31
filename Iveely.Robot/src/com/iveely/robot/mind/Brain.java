/**
 * date   : 2016年1月28日
 * author : Iveely Liu
 * contact: sea11510@mail.ustc.edu.cn
 */
package com.iveely.robot.mind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.css.Rect;

import com.iveely.robot.daiml.Category;
import com.iveely.robot.environment.Branch;
import com.iveely.robot.environment.Variable;
import com.iveely.robot.index.Inverted;
import com.iveely.robot.mind.Nerve.EventHandler;
import com.iveely.robot.mind.React.Status;
import com.iveely.robot.net.websocket.WSHandler;
import com.iveely.robot.util.Serialize;
import com.iveely.robot.util.StringUtil;

/**
 * @author {Iveely Liu}
 *
 */
public class Brain {

	/**
	 * All set information.
	 */
	private HashMap<String, Set<Integer>> _set;

	/**
	 * All categories.
	 */
	private List<Category> _categories;

	/**
	 * All branches.
	 */
	private Map<String, Branch> _branches;

	/**
	 * All sessions of talk.
	 */
	private HashMap<Integer, Session> _sessions;

	/**
	 * Logger.
	 */
	private static Logger _logger;

	/**
	 * Single instance.
	 */
	private static Brain _brain;

	private Brain() {
		this._set = new HashMap<>();
		this._categories = new ArrayList<>();
		Brain._logger = Logger.getLogger(Brain.class);
		this._sessions = new HashMap<>();
		this._branches = Serialize.fromXML(Variable.getPathOfBranch());
	}

	/**
	 * Get instance of set brain.
	 * 
	 * @return instance of set brain.
	 */
	public static Brain getInstance() {
		if (_brain == null) {
			synchronized (Brain.class) {
				_brain = new Brain();
			}
		}
		return _brain;
	}

	/**
	 * Add set information.
	 * 
	 * @param key
	 *            The key of set.
	 * @param val
	 *            The value of set.
	 */
	public void addSet(String key, List<String> val) {
		if (!this._set.containsKey(key)) {
			Set<Integer> list = new HashSet<>();
			for (String v : val) {
				Integer code = v.hashCode();
				if (!list.contains(code)) {
					list.add(code);
				}
			}
			this._set.put(key, list);
			Brain._logger.info(String.format("Set's name '%s' has beed installed.", key));
		} else {
			Brain._logger.error(String.format("Error:same name of set by %s.", key));
		}
	}

	/**
	 * Add category into brain.
	 * 
	 * @param category
	 */
	public void addCategory(Category category) {
		this._categories.add(category);
		Inverted.getInstance().set(this._categories.size() - 1, category.getIndex());
	}

	/**
	 * Check is a value in set by key's name.
	 * 
	 * @param key
	 *            The name of the key.
	 * @param val
	 *            The value to be checked.
	 * @return True is in a set, or is not.
	 */
	public boolean isInSet(String key, String val) {
		if (this._set.containsKey(key)) {
			Integer code = val.hashCode();
			return this._set.get(key).contains(code);
		}
		return false;
	}

	/**
	 * Think the question to find the answer.
	 * 
	 * @param question
	 *            The question that user input.
	 * @return the result what robot think.
	 */
	public String think(String question) {

		List<Integer> list = Inverted.getInstance().get(question);
		if (list == null || list.size() == 0) {
			return null;
		} else {
			React react;
			for (Integer id : list) {
				react = _categories.get(id).getAnwser(question);
				if (react.getStatus() == Status.SUCCESS) {
					return react.getRet();
				} else if (react.getStatus() == Status.RECURSIVE) {
					return think(react.getRet());
				} else {
					continue;
				}
			}
		}
		return "Not found.";
	}

	/**
	 * A session request to brain.
	 * 
	 * @param sessionId
	 *            session id of current request.
	 * @param handler
	 * @param question
	 */
	public void request(int sessionId, EventHandler handler, String question) {
		Session session;
		if (this._sessions.containsKey(sessionId)) {
			session = this._sessions.get(sessionId);
			session.setQuestion(question);
		} else {
			session = new Session(handler, question);
			this._sessions.put(sessionId, session);
		}
		// TODO:Use thread pool.
		new Thread(session).start();
	}

	/**
	 * Release a session.
	 * 
	 * @param sessionId
	 */
	public void release(int sessionId) {
		if (this._sessions.containsKey(sessionId)) {
			this._sessions.remove(sessionId);
		}
	}
}
