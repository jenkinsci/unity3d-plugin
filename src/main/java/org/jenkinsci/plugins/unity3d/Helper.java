package org.jenkinsci.plugins.unity3d;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import hudson.util.ArgumentListBuilder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author rstyrczula
 */
public class Helper {
    public static Set<Integer> toIntegerSet(String csvIntegers) {
        Set<Integer> result = new HashSet<Integer>();
        if (! csvIntegers.trim().equals("")) {
            result.addAll(Collections2.transform(Arrays.asList(csvIntegers.split(",")), new Function<String, Integer>() {
                public Integer apply(String s) {
                    return Integer.parseInt(s.trim());
                }
            }));
        }
        return result;
    }

    public static String findCommandlineArgument(ArgumentListBuilder args, String arg) {
        return findArgFromList(args.toList(), arg);
    }

    public static String findArgFromList(List<String> a, String arg) {
        String customArg = null;
        for (int i = 0; i < a.size() - 1; i++) {
            if (a.get(i).equals(arg)) {
                customArg = a.get(i+1);
            }
        }
        return customArg;
    }
}
