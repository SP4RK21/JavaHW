package ru.ifmo.rain.tynyanov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    private List<String> collectStudentParameters(List<Student> students, Function<Student, String> function) {
        return studentParametersStream(students, function).collect(Collectors.toList());
    }

    private Stream<String> studentParametersStream(List<Student> students, Function<Student, String> function) {
        return students.stream().map(function);
    }

    private Comparator<Student> studentsComparator =
            Comparator.comparing(Student::getLastName, String::compareTo).
                    thenComparing(Student::getFirstName, String::compareTo).
                    thenComparing(Student::getId, Integer::compareTo);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return collectStudentParameters(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return collectStudentParameters(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return collectStudentParameters(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return collectStudentParameters(students, s -> s.getFirstName() + " " + s.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return studentParametersStream(students, Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream().min(Student::compareTo)
                .map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Student::compareTo)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(studentsComparator)
                .collect(Collectors.toList());
    }

    private List<Student> findStudentsBy(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream().filter(predicate)
                .sorted(studentsComparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, s -> s.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, s -> s.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, s -> s.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream().sorted(studentsComparator)
                .filter(s -> s.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}
