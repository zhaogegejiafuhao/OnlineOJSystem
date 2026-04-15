package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.Teacher;
import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.entity.UserProfile;
import cn.edu.zjnu.acm.entity.oj.Solution;
import cn.edu.zjnu.acm.entity.oj.UserProblem;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.repo.user.UserRepository;
import cn.edu.zjnu.acm.repo.problem.SolutionRepository;
import cn.edu.zjnu.acm.repo.user.UserProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TeacherRepository teacherRepository;
    private final SolutionRepository solutionRepository;
    private final UserProblemRepository userProblemRepository;

    @PostConstruct
    public void AddAdministratorAccount(){
        if (!userRepository.findByUsername("administrator").isPresent()){
            String password = System.getenv("ADMIN_PASSWORD");
            if (password==null){
                password="123456";
            }
            User admin = new User("administrator",password,"Administrator","email@address.com","");
            admin = registerUser(admin);
            Teacher teacher = new Teacher(admin,Teacher.ADMIN);
            teacherRepository.save(teacher);
        }
    }

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository, TeacherRepository teacherRepository, SolutionRepository solutionRepository, UserProblemRepository userProblemRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.teacherRepository = teacherRepository;
        this.solutionRepository = solutionRepository;
        this.userProblemRepository = userProblemRepository;
    }

    public Page<User> searchUser(int page, int size, String search) {
        return userRepository.findAllByUsernameContains(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")), search);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public User registerUser(User u) {
        if (userRepository.findByUsername(u.getUsername()).isPresent())
            return null;
        u = setUserPassword(u,u.getPassword());
        UserProfile userProfile = new UserProfile();
        u = userRepository.save(u);
        if (u == null)
            return null;
        userProfile.setUser(u);
        userProfileRepository.save(userProfile);
        return u;
    }

    public User setUserPassword(User u, String pwd) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        u.setPassword(encoder.encode(pwd));
        return u;
    }

    public void updateUserInfo(User user) {
        userRepository.updateUser(user.getId(), user.getName(), user.getPassword(), user.getEmail(), user.getIntro());
    }

    public boolean checkPassword(String password, String correct) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, correct);
    }

    public User loginUser(User user) {
        User u = userRepository.findByUsername(user.getUsername()).orElse(null);
        if (u == null)
            return null;
        if (checkPassword(user.getPassword(), u.getPassword()))
            return u;
        return null;
    }

    public List<User> userList() {
        List<User> userList = userRepository.findAll();
        return userList;
    }
    
    /**
     * Add new user
     */
    @Transactional
    public User addUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return null;
        }
        user = setUserPassword(user, user.getPassword());
        UserProfile userProfile = new UserProfile();
        user = userRepository.save(user);
        if (user == null) {
            return null;
        }
        userProfile.setUser(user);
        userProfileRepository.save(userProfile);
        return user;
    }
    
    /**
     * Update user information
     */
    @Transactional
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser == null) {
            return null;
        }
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setIntro(user.getIntro());
        return userRepository.save(existingUser);
    }
    
    /**
     * Reset user password
     */
    @Transactional
    public boolean resetUserPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        user = setUserPassword(user, newPassword);
        userRepository.save(user);
        return true;
    }

    /**
     * get user's permission
     *
     * @param user
     * @return -1 if normal users, otherwise return teacher privileges.
     */
    public int getUserPermission(User user) {
        if (!teacherRepository.existsByUser(user))
            return -1;
        return teacherRepository.findByUser(user).get().getPrivilege();
    }
    
    /**
     * Delete user with cascading deletion of related data
     */
    public void deleteUser(User user) {
        // Delete user submissions
        solutionRepository.deleteAllByUser(user);
        
        // Delete user problem relationships
        userProblemRepository.deleteAllByUser(user);
        
        // Delete user profile
        userProfileRepository.deleteAllByUser(user);
        
        // Delete teacher privileges if exists
        teacherRepository.deleteByUser(user);
        
        // Delete user itself
        userRepository.delete(user);
    }
    
    /**
     * Batch delete users
     */
    public int deleteUsers(List<Long> userIds) {
        int deletedCount = 0;
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                deleteUser(user);
                deletedCount++;
            }
        }
        return deletedCount;
    }
}
