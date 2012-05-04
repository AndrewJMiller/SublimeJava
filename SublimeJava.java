/*
Copyright (c) 2012 Fredrik Ehnbom

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

   1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required.

   2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.

   3. This notice may not be removed or altered from any source
   distribution.
*/
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


public class SublimeJava
{
    public static String[] getCompletion(Method m, String filter)
    {
        String str = m.getName();
        if (!str.startsWith(filter))
            return null;
        str += "(";
        String ins = str;
        int count = 1;
        for (Class c2 : m.getParameterTypes())
        {
            if (count > 1)
            {
                str += ", ";
                ins += ", ";
            }
            String n = c2.getName();
            str += n;
            ins += "${"+count + ":" + n + "}";
            count++;
        }
        str += ")\t" + m.getReturnType().getName();
        ins += ")";
        return new String[] {str, ins};
    }
    public static String[] getCompletion(Field f, String filter)
    {
        String str = f.getName();
        if (!str.startsWith(filter))
            return null;

        String rep = str + "\t" + f.getType().getName();
        return new String[] {rep, str};
    }
    private static final String sep = ";;--;;";

    private static String getClassname(String pack, String clazz)
    {
        if (pack.endsWith(".*"))
        {
            return pack.substring(0, pack.length()-2) + "." + clazz;
        }
        else if (pack.length() != 0)
        {
            return pack + "$" + clazz;
        }
        return clazz;
    }

    public static void main(String... unusedargs)
    {
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            boolean first = true;
            while (true)
            {
                if (!first)
                    // Just to indicate that there's no more output from the command and we're ready for new input
                    System.out.println(";;--;;");
                first = false;
                String cmd = in.readLine();
                String args[] = cmd.split(" ");
                System.err.println(args.length);
                for (int i = 0; i < args.length; i++)
                {
                    System.err.println(args[i]);
                }

                try
                {
                    if (args[0].equals("-quit"))
                    {
                        System.err.println("quitting upon request");
                        return;
                    }
                    else if (args[0].equals("-separator"))
                    {
                        System.out.println(System.getProperty("path.separator"));
                        continue;
                    }
                    else if (args[0].equals("-findclass"))
                    {
                        String line = null;
                        ArrayList<String> packages = new ArrayList<String>();
                        try
                        {
                            while ((line = in.readLine()) != null)
                            {
                                if (line.compareTo(sep) == 0)
                                    break;
                                System.err.println(line);
                                System.err.println(line == sep);
                                packages.add(line);
                            }
                        }
                        catch (Exception e)
                        {
                        }
                        for (String pack : packages)
                        {
                            try
                            {
                                Class c = Class.forName(getClassname(pack, args[1]));
                                System.out.println("" + c.getName());
                                continue;
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        // Still haven't found anything, so try to see if it's an internal class
                        for (String pack : packages)
                        {
                            String classname = getClassname(pack, args[1]);
                            while (classname.indexOf('.') != -1)
                            {
                                int idx = classname.lastIndexOf('.');
                                classname = classname.substring(0, idx) + "$" + classname.substring(idx+1);
                                try
                                {
                                    Class c = Class.forName(classname);
                                    System.out.println("" + c.getName());
                                    continue;
                                }
                                catch (Exception e)
                                {
                                }
                            }
                        }
                        continue;
                    }
                    if (args.length < 2)
                        continue;
                    Class<?> c = Class.forName(args[1]);
                    String filter = "";
                    if (args.length >= 3)
                        filter = args[2];
                    if (args[0].equals("-complete"))
                    {
                        for (Field f : c.getFields())
                        {
                            String[] completion = getCompletion(f, filter);
                            if (completion == null)
                                continue;
                            System.out.println(completion[0] + sep + completion[1]);
                        }
                        for (Method m : c.getMethods())
                        {
                            String[] completion = getCompletion(m, filter);
                            if (completion == null)
                                continue;
                            System.out.println(completion[0] + sep + completion[1]);
                        }
                        for (Class clazz : c.getClasses())
                        {
                            System.out.println(clazz.getSimpleName() + "\tclass" + sep + clazz.getSimpleName());
                        }
                    }
                    else if (args[0].equals("-returntype"))
                    {
                        boolean cont = false;
                        for (Field f : c.getFields())
                        {
                            if (filter.equals(f.getName()))
                            {
                                System.out.println("" + f.getType().getName());
                                cont = true;
                                break;
                            }
                        }
                        if (cont)
                            continue;
                        for (Method m : c.getMethods())
                        {
                            if (filter.equals(m.getName()))
                            {
                                System.out.println("" + m.getReturnType().getName());
                                break;
                            }
                        }
                        if (cont)
                            continue;
                        for (Class clazz : c.getClasses())
                        {
                            if (filter.equals(clazz.getSimpleName()))
                            {
                                System.out.println(clazz.getName());
                                break;
                            }
                        }
                    }
                    else if (args[0].equals("-cache"))
                    {
                        String source = "unknown";
                        try
                        {
                            URL u = c.getResource("/" + c.getName().replace('.', '/') + ".class");
                            source = u.toString();
                        }
                        catch (Exception e)
                        {
                        }
                        System.out.println(c.getName() + sep + source);
                        for (Field f : c.getFields())
                        {
                            String[] comp = getCompletion(f, "");
                            System.out.println("0" + sep + f.getType().getName() + sep +
                                                      f.getModifiers() + sep +
                                                      comp[0] + sep +
                                                      comp[1]);
                        }
                        for (Method m : c.getMethods())
                        {
                            String[] comp = getCompletion(m, "");
                            System.out.println("1" + sep + m.getReturnType().getName() + sep +
                                                      m.getModifiers() + sep +
                                                      comp[0] + sep +
                                                      comp[1]);
                        }
                        for (Class clazz : c.getClasses())
                        {
                            System.out.println("2" + sep + clazz.getName() + sep +
                                                            clazz.getModifiers() + sep +
                                                            clazz.getSimpleName() + "\tclass" + sep +
                                                            clazz.getSimpleName());
                        }
                    }
                }
                catch (ClassNotFoundException x)
                {
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
