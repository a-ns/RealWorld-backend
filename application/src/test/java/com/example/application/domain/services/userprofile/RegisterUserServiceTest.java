package com.example.application.domain.services.userprofile;

import static org.junit.jupiter.api.Assertions.*;

import com.example.application.domain.exceptions.EmailAreadyTakenException;
import com.example.application.domain.exceptions.RegistrationValidationException;
import com.example.application.domain.exceptions.UsernameAlreadyTakenException;
import com.example.application.domain.model.User;
import com.example.application.domain.model.UserRegistrationCommand;
import com.example.application.domain.ports.out.AuthPort;
import com.example.application.domain.ports.out.GetUserPort;
import com.example.application.domain.ports.out.SaveUserPort;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

  @InjectMocks RegisterUserService sut;
  @Mock GetUserPort getUserPort;
  @Mock AuthPort authService;
  @Mock SaveUserPort saveUserPort;
  @Mock UserRegistrationValidator validator;

  @Test
  @DisplayName("a user is registered")
  void register_user() {
    // Arrange
    String username = "bob";
    String email = "hello@world.com";
    String password = "password";
    String encrypted_password = "encrypted_password";
    Integer id = 1234;
    String token = "abc123";
    User user = User.builder().password(encrypted_password).email(email).username(username).build();
    User userWithId =
        User.builder().password(encrypted_password).email(email).username(username).id(id).build();
    UserRegistrationCommand registrant =
        UserRegistrationCommand.builder()
            .email(email)
            .password(password)
            .username(username)
            .build();
    Mockito.when(getUserPort.getUserByUsername(username)).thenReturn(Optional.empty());
    Mockito.when(getUserPort.getUserByEmail(email)).thenReturn(Optional.empty());
    Mockito.when(authService.encrypt(password)).thenReturn(encrypted_password);
    Mockito.when(saveUserPort.saveUser(user)).thenReturn(userWithId);
    Mockito.when(authService.generateToken(userWithId)).thenReturn(token);
    // Act
    User actual = sut.registerUser(registrant);
    // Assert

    User expected =
        User.builder()
            .password(encrypted_password)
            .email(email)
            .username(username)
            .id(id)
            .token(token)
            .build();

    Assertions.assertEquals(expected, actual);
  }

  @Test
  @DisplayName(
      "An existing user is found with the desired username to the new user is not registered")
  void user_with_desired_username_already_exists_so_throws_exception() {
    // Arrange
    String username = "bob";
    String email = "hello@world.com";
    String password = "password";
    String encrypted_password = "encrypted_password";
    Integer id = 1234;

    User existingUser =
        User.builder().password(encrypted_password).email(email).username(username).id(id).build();
    Mockito.when(getUserPort.getUserByUsername(username)).thenReturn(Optional.of(existingUser));
    // Act & Assert
    Assertions.assertThrows(
        UsernameAlreadyTakenException.class,
        () ->
            sut.registerUser(
                UserRegistrationCommand.builder()
                    .email(email)
                    .password(password)
                    .username(username)
                    .build()));
    Mockito.verifyNoInteractions(this.saveUserPort);
  }

  @Test
  @DisplayName("An existing user is found with the desired email so the new user is not registered")
  void existing_user_found_with_desired_email() {
    // Arrange
    String username = "bob";
    String email = "hello@world.com";
    String password = "password";
    String encrypted_password = "encrypted_password";
    Integer id = 1234;
    String token = "abc123";
    User foundUser =
        User.builder().password(encrypted_password).email(email).username(username).id(id).build();
    Mockito.when(getUserPort.getUserByUsername(username)).thenReturn(Optional.empty());
    Mockito.when(getUserPort.getUserByEmail(email)).thenReturn(Optional.of(foundUser));
    // Act
    Assertions.assertThrows(
        EmailAreadyTakenException.class,
        () ->
            sut.registerUser(
                UserRegistrationCommand.builder()
                    .email(email)
                    .password(password)
                    .username(username)
                    .build()));
    Mockito.verifyNoInteractions(this.saveUserPort);
  }

  @Test
  @DisplayName("Registration does not pass validation so user is not registered")
  void does_not_pass_valiation_so_user_is_not_registered() {
    // Arrange
    String username = "bob";
    String email = "hello@world.com";
    String password = "password";
    String encrypted_password = "encrypted_password";
    Integer id = 1234;
    String token = "abc123";
    User foundUser =
        User.builder().password(encrypted_password).email(email).username(username).id(id).build();
    UserRegistrationCommand registrant =
        UserRegistrationCommand.builder()
            .email(email)
            .password(password)
            .username(username)
            .build();
    Mockito.doThrow(RegistrationValidationException.class).when(validator).validate(registrant);
    // Act
    Assertions.assertThrows(
        RegistrationValidationException.class, () -> sut.registerUser(registrant));
    Mockito.verifyNoInteractions(this.saveUserPort);
  }
}
