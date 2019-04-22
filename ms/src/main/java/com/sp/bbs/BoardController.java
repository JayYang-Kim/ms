package com.sp.bbs;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.sp.common.MyUtil;

/*
 - mysql-connector-java 버전 5.1.X 이후 버전부터 KST 타임존을 인식하지 못하는 경우
    1) url에 serverTimezone 추가
        jdbc.url=jdbc:mysql://localhost:3306/mydb?characterEncoding=UTF-8&serverTimezone=UTC
    2) The reference to entity "serverTimezone" must end with the ';' delimiter.  에러가 발생할 경우 & 대신에 &amp;  사용
         jdbc.url=jdbc:mysql://localhost:3306/mydb?characterEncoding=UTF-8&amp;serverTimezone=UTC
*/

@Controller("bbs.boardController")
public class BoardController {
	
	@Autowired
	private BoardService boardService;
	@Autowired
	private MyUtil myUtil;
	
	@RequestMapping(value="bbs/list")
	public String list(@RequestParam(value="page", defaultValue="1") int current_page,
			@RequestParam(defaultValue="all") String condition,
			@RequestParam(defaultValue="") String keyword,
			HttpServletRequest req,
			Model model) throws Exception {
		
		if(req.getMethod().equalsIgnoreCase("GET")) {
			keyword = URLDecoder.decode(keyword, "UTF-8");
		}
		
		int dataCount = 0;
		int total_page = 0;
		int rows = 10;
		
		Map<String, Object> map = new HashMap<>();
		map.put("condition", condition);
		map.put("keyword", keyword);
		
		dataCount = boardService.dataCount(map);
		
		if(dataCount != 0) {
			total_page = myUtil.pageCount(rows, dataCount);
		}

		if(current_page > total_page) {
			current_page = total_page;
		}
		
		int start = (current_page - 1) * rows;
		if(start < 0) {
			start = 0;
		}
		
		map.put("start", start);
		map.put("rows", rows);
		
		List<Board> list = boardService.listBoard(map);
		
		int listNum, n = 0;
		for(Board dto : list) {
			listNum = dataCount - (start + n);
			dto.setListNum(listNum);
			n++;
		}
		
		String cp = req.getContextPath();
		String query = "";
		String listUrl = cp + "/bbs/list";
		String articleUrl = cp + "/bbs/article?page=" + current_page;
		
		if(condition.length()!=0) {
			query = "condition=" + condition + "&keyword=" + URLEncoder.encode(keyword, "UTF-8");
			
			listUrl += "?" + query;
			articleUrl += "&" + query;
		}
		
		String paging = myUtil.paging(current_page, total_page, listUrl);
		
		model.addAttribute("list", list);
		model.addAttribute("articleUrl", articleUrl);
		model.addAttribute("dataCount", dataCount);
		model.addAttribute("page", current_page);
		model.addAttribute("total_page", total_page);
		model.addAttribute("paging", paging);
		model.addAttribute("condition", condition);
		model.addAttribute("keyword", keyword);
		
		return "bbs/list";
	}
	
	@RequestMapping(value="bbs/created", method = RequestMethod.GET)
	public String bbs(Model model) {
		model.addAttribute("mode", "created");

		return "bbs/created";
	}
	
	@RequestMapping(value="bbs/created", method = RequestMethod.POST)
	public String bbs(Board dto, HttpServletRequest req) {
		dto.setIpAddr(req.getRemoteAddr());
		
		boardService.insertBoard(dto);
		
		return "redirect:/bbs/list";
	}
	
	@RequestMapping(value="bbs/article")
	public String article(@RequestParam int num,
			@RequestParam(defaultValue="1") int page,
			@RequestParam(defaultValue="all") String condition,
			@RequestParam(defaultValue="") String keyword,
			HttpServletRequest req,
			Model model) throws Exception {
		
		if(req.getMethod().equalsIgnoreCase("GET")) {
			keyword = URLDecoder.decode(keyword, "UTF-8");
		}
		
		boardService.updateHitCount(num);
		
		Board dto = boardService.readBoard(num);
		
		dto.setContent(myUtil.htmlSymbols(dto.getContent()));
		
		Map<String, Object> map = new HashMap<>();
		map.put("condition", condition);
		map.put("keyword", keyword);
		map.put("num", num);
		
		Board preReadBoard = boardService.preReadBoard(map);
		Board nextReadBoard = boardService.nextReadBoard(map);
		
		String query = "page=" + page;
		
		if(condition.length()!=0) {
			query = "&condition=" + condition + "&keyword=" + URLEncoder.encode(keyword, "UTF-8");
		}
		
		model.addAttribute("dto", dto);
		model.addAttribute("query", query);
		model.addAttribute("preReadDto", preReadBoard);
		model.addAttribute("nextReadDto", nextReadBoard);
		model.addAttribute("page", page);
		
		return "bbs/article";
	}
	
	@RequestMapping(value="/bbs/update", method = RequestMethod.GET)
	public String updateBoard(@RequestParam int num,
			@RequestParam(defaultValue = "1") int page,
			Model model) throws Exception {
		
		Board dto = boardService.readBoard(num);
		
		model.addAttribute("dto", dto);
		model.addAttribute("page", page);
		model.addAttribute("mode", "update");
		
		return "bbs/created";
	}
	
	@RequestMapping(value="/bbs/update", method = RequestMethod.POST)
	public String updateSubmit(Board dto,
			@RequestParam int num,
			@RequestParam(defaultValue = "1") int page,
			HttpServletRequest req) throws Exception {
		
		if(req.getMethod().equalsIgnoreCase("GET")) {
			return "redirect:/bbs/list?page=" + page;
		}
		
		boardService.updateBoard(dto);
		
		return "redirect:/bbs/list?page=" + page;
	}
	
	@RequestMapping(value="/bbs/delete")
	public String deleteBoard(@RequestParam int num,	
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "all") String condition,
			@RequestParam(defaultValue = "") String keyword,
			HttpServletRequest req) throws Exception {
		
		if(req.getMethod().equalsIgnoreCase("GET")) {
			keyword = URLDecoder.decode(keyword, "UTF-8");
		}
		
		String query = "page=" + page;
		
		if(condition.length()!=0) {
			query = "&condition=" + condition + "&keyword=" + URLEncoder.encode(keyword, "UTF-8");
		}
		
		boardService.deleteBoard(num);
		
		return "redirect:/bbs/list?" + query;
	}
	
}
