package com.tenco.blog.user;

// 디비 접근 쿼리, 트랜잭션

import com.tenco.blog._core.errors.exception.Exception400;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor // 생성자 의존 주입 - DI 처리
@Repository // IoC 대상 + 싱글톰 패턴으로 관리 됨
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    // 생성자 의존 주입 - DI
    private final EntityManager em;

    // 회원 정보 수정기능
    @Transactional
    public User updateById(Long id, UserRequest.UpdateDTO reqDTO) {
        log.info("회원 정보 수정 시작 ID : {}", id);
        // 조회 -> 객체의 상태값 변경 -> 트랜잭션 처리 ---> update
        User user = findById(id);
        //객체의 상태값을 행위를 통해서 변경
        user.setPassword(reqDTO.getPassword());
        // 수정된 영속 엔티티 반환(세션 동가화 용)
        return user;
    }

    /**
     * 로그인 요청 기능 (사용자 정보 조회 - username, password)
     *
     * @param username
     * @param password
     * @return 성공시 User 엔티티, 실패시 null 반환
     */
    public User findByUsernameAndPassword(String username, String password) {
        // JPQL
        // 필요하다면 직접 예외 처리 설정
        try {
            String jpql = "SELECT u FROM User u " +
                    "WHERE u.username = :username AND u.password = :password ";
            TypedQuery typedQuery = em.createQuery(jpql, User.class);
            typedQuery.setParameter("username", username);
            typedQuery.setParameter("password", password);
            return (User) typedQuery.getSingleResult();
        } catch (Exception e) {
            // 일치하는 사용자가 없거나 에러 발생 시 null 반환
            // 즉, 로그인 실패를 의미함
            return null;
        }
    }

    /**
     * 회원정보 저장 처리
     *
     * @param user (비영속 상태)
     * @return user 엔티티 반환
     */
    @Transactional // select 제외하고 걸어주기
    public User save(User user) {
        // 매개 변수에 들어오는 user object 는 비영속화 된 상태이다.
        log.info("회원 정보 저장 시작");
        em.persist(user); // 영속성 컨텍스트에 user 객체 관리하기 시작함

        // 트랜잭션이 끝나면 커밋 시점에 실제 INSERT 쿼리를 실행한다.
        return user;
    }

    // 사용자명 중복 체크용 조회 기능
    public User findByUsername(String username) {

//        // where username = ?
//        // select --> 네이티브 쿼리, JPQL 중 선택 (JPQL 사용)
//        String jpql = "SELECT u FROM User u WHERE u.username = :username ";
//        TypedQuery<User> typedQuery = em.createQuery(jpql, User.class); // User.class 타입의 안정성
//        typedQuery.setParameter("username", username);
//        return typedQuery.getSingleResult();

        log.info("중복 사용자 이름 조회");
        // 필요하다면 직접 예외 처리
        try {
            String jpql = "SELECT u FROM User u WHERE u.username = :username ";
            return em.createQuery(jpql, User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    // 회원 정보 단 건 조회기능
    public User findById(Long id) {
        log.info("사용자 조회 - ID : {}", id);
        User user = em.find(User.class, id);
        if (user == null) {
            throw new Exception400("사용자를 찾을 수 없습니다");
        }
        return user;
    }
}
