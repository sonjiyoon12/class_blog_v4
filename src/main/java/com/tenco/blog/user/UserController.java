package com.tenco.blog.user;

import com.tenco.blog._core.errors.exception.Exception400;
import com.tenco.blog._core.errors.exception.Exception401;
import com.tenco.blog._core.errors.exception.Exception403;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor // DI 처리
@Controller
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    // httpSession <-- 세션 메모리에 접근 할 수 있다. WAS 단에 있음
    private final HttpSession httpSession;

    // 회원 정보 보기 화면 요청
    // 주소 설계 : http://localhost:8080/user/update-form
    @GetMapping("/user/update-form")
    public String updateForm(HttpServletRequest request, HttpSession session) {

        log.info("회원 정보 수정 폼 요청");
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            throw new Exception403("로그인이 필요한 서비스 입니다");
        }
        request.setAttribute("user", sessionUser);

        return "user/update-form";
    }

    // 회원 정보 수정 액션 처리
    @PostMapping("/user/update")
    public String update(UserRequest.UpdateDTO reqDTO,
                         HttpSession session, HttpServletRequest request) {

        log.info("회원 정보 수정 요청");
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            throw new Exception403("로그인이 필요한 서비스 입니다");
        }

        // 데이터 유효성 검사 처리
        reqDTO.validate();

        // 회원 정보 수정은 권한 체크가 필요 없다 (세션에서 정보를 가져오기 때문)
        User updateUser = userRepository.updateById(sessionUser.getId(), reqDTO);

        // 세션 동기화
        session.setAttribute("sessionUser", updateUser);

        // 다시 회원 정보 보기 화면 요청
        return "redirect:/user/update-form";
    }

    /**
     * 회원 가입 화면 요청
     *
     * @return join-form.mustache
     */
    @GetMapping("/join-form")
    public String joinForm() {
        log.info("회원 가입 요청 폼");
        return "user/join-form";
    }

    // 회원 가입 액션 처리
    @PostMapping("/join")
    public String join(UserRequest.JoinDTO joinDTO, HttpServletRequest request) {
        log.info("회원 가입 기능 요청");
        log.info("사용자 명 : {} ", joinDTO.getUsername());
        log.info("사용자 이메일 : {} ", joinDTO.getEmail());

            // 1. 입력된 데이터 검증(유효성 검사)
            joinDTO.validate();

            // 2. 사용자명 중복 체크
            User existUser = userRepository.findByUsername(joinDTO.getUsername());
            if (existUser != null) {
                throw new Exception401("이미 존재하는 사용자명 입니다 "
                        + joinDTO.getUsername());
            }

            // 3. DTO를 User Object로 변환
            User user = joinDTO.toEntity();

            // 4. User Object를 영속화 처리
            userRepository.save(user);

            return "redirect:/login-form";
        }

    /**
     * 로그인 화면 요청
     *
     * @return login-form.mustache
     */
    @GetMapping("/login-form")
    public String loginForm() {
        log.info("로그인 요청 폼");
        // 반환값이 뷰(파일) 이름이 됨 (뷰 리졸버가 실제 파일 경로를 찾아 감)
        return "user/login-form";
    }

    // 로그인 액션 처리
    // 자원의 요청은 GET 방식이다. 단 로그인 요청은 예외 (주소창에 뜨기 때문)
    // 보안상 이유

    // DTO 패턴 활용
    // 1. 입력 데이터 검증
    // 2. 정상 데이터라면 사용자명과 비밀번호를 DB 접근해서 조회
    // 3. 로그인 성공/실패 처리
    // 4. 로그인 성공이라면 서버측 메모리에 사용자 정보를 저장
    // 5. 메인 화면으로 리다이렉트 처리
    @PostMapping("/login")
    public String login(UserRequest.LoginDTO loginDTO) {

        log.info("=== 로그인 시도 ===");
        log.info("사용자명 :  {}", loginDTO.getUsername());

            // 1.
            loginDTO.validate();
            // 2.
            User user = userRepository.findByUsernameAndPassword(loginDTO.getUsername(),
                    loginDTO.getPassword());
            // 3. 로그인 실패
            if (user == null) {
                // 로그인 실패: 일치된 사용자 없음
                throw new Exception400("사용자명 또는 비밀번호가 틀렸어");
            }
            // 4. 로그인 성공
            httpSession.setAttribute("sessionUser", user);

            // 5. 로그인 성공 후 리스트 페이지 이동
            return "redirect:/";
        }

    // 로그아웃 처리
    @GetMapping("/logout")
    public String logout() {
        log.info("=== 로그아웃 ===");
        // 세션 무효화
        httpSession.invalidate();
        return "redirect:/";
    }
}
