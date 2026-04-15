package cn.edu.zjnu.acm.service;

import cn.edu.zjnu.acm.entity.User;
import cn.edu.zjnu.acm.repo.user.TeacherRepository;
import cn.edu.zjnu.acm.repo.user.UserProfileRepository;
import cn.edu.zjnu.acm.repo.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private UserService userService;

    @Test
    public void testRegisterUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setName("Test User");
        user.setEmail("test@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.registerUser(user);
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    public void testRegisterUser_UsernameExists() {
        User user = new User();
        user.setUsername("existinguser");
        user.setPassword("password123");

        when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(user));

        User result = userService.registerUser(user);
        assertNull(result);
    }

    @Test
    public void testResetUserPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("oldpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        boolean result = userService.resetUserPassword(1L, "newpassword");
        assertTrue(result);
    }

    @Test
    public void testResetUserPassword_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        boolean result = userService.resetUserPassword(1L, "newpassword");
        assertFalse(result);
    }
}